package com.ghostaccess.scanner

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : android.app.Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var content: LinearLayout
    private lateinit var homeTab: Button
    private lateinit var appsTab: Button
    private lateinit var filesTab: Button
    private var screen = Screen.HOME
    private var appFilter = AppFilter.REVIEW
    private var latestDeviceResult: ScanResult? = null
    private var latestFileResult: FileScanResult? = null
    private var deviceScanInProgress = false
    private var fileScanInProgress = false
    private var waitingForUsageAccess = false

    private enum class Screen { HOME, APPS, FILES }
    private enum class AppFilter { REVIEW, ALL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = color(R.color.canvas)
        window.navigationBarColor = color(R.color.surface)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(createScreen())
        if (!hasUsageAccess()) content.post { explainUsageAccess() }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForUsageAccess) {
            waitingForUsageAccess = false
            if (hasUsageAccess()) runDeviceScan() else render()
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            FILE_PICKER_REQUEST -> {
                val uris = buildList {
                    data.data?.let(::add)
                    data.clipData?.let { clip -> repeat(clip.itemCount) { add(clip.getItemAt(it).uri) } }
                }.distinct()
                if (uris.isNotEmpty()) scanSelectedFiles(uris)
            }
            FOLDER_PICKER_REQUEST -> data.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) { }
                scanFolder(uri)
            }
        }
    }

    private fun createScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.canvas))
        }
        root.addView(topBar())

        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(26))
        }
        scroll.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(bottomBar())
        render()
        return root
    }

    private fun topBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(22), dp(18), dp(16), dp(11))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("GhostAccess", 27, R.color.ink, true))
            addView(text("PRIVATE DEVICE SECURITY", 10, R.color.muted, true).letterSpacing(0.11f).margins(top = 2))
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        addView(iconButton("i", "How GhostAccess works") { showAbout() }, LinearLayout.LayoutParams(dp(42), dp(42)))
    }

    private fun bottomBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(10), dp(12), dp(18))
        setBackgroundColor(color(R.color.surface))
        homeTab = navButton("⌂\nHome") { screen = Screen.HOME; render() }
        appsTab = navButton("◌\nApps") { screen = Screen.APPS; render() }
        filesTab = navButton("⌁\nFiles") { screen = Screen.FILES; render() }
        addView(homeTab, LinearLayout.LayoutParams(0, dp(52), 1f))
        addView(appsTab, LinearLayout.LayoutParams(0, dp(52), 1f))
        addView(filesTab, LinearLayout.LayoutParams(0, dp(52), 1f))
    }

    private fun render() {
        content.removeAllViews()
        updateNav()
        when (screen) {
            Screen.HOME -> renderHome()
            Screen.APPS -> renderApps()
            Screen.FILES -> renderFiles()
        }
    }

    private fun renderHome() {
        val result = latestDeviceResult
        val score = result?.let { maxOf(55, 100 - minOf(45, it.findings.sumOf { finding -> finding.riskScore * 2 })) }
        content.addView(heroCard(score, result).margins(bottom = 16))
        content.addView(primaryButton(if (deviceScanInProgress) "Checking your device…" else "Run device scan").apply {
            isEnabled = !deviceScanInProgress
            setOnClickListener { if (hasUsageAccess()) runDeviceScan() else explainUsageAccess() }
        }.margins(bottom = 10))
        content.addView(outlineButton("Scan documents or a folder") { screen = Screen.FILES; render() }.margins(bottom = 25))

        sectionHeading("Smart advice", "Based on your real device")
        when {
            !hasUsageAccess() -> adviceCard("Permission needed", "Enable Usage Access so GhostAccess can identify apps that have not been used recently but still have sensitive permissions.", "Open settings") { explainUsageAccess() }
            deviceScanInProgress -> infoCard("Checking your device", "Reading Android's installed-app metadata and usage summaries. Nothing leaves your phone.")
            result == null -> infoCard("Your first check is ready", "Run a device scan to get advice based on your installed apps—not generic examples.")
            result.findings.isEmpty() -> {
                adviceCard("Your permission hygiene looks good", "No unused app with sensitive permissions was found. Keep checking after installing or deleting apps.", "Scan again") { runDeviceScan() }
                infoCard("A small habit that helps", "Review Camera, Microphone, Location, Contacts, and SMS after you stop using an app.")
            }
            else -> result.findings.take(3).forEach { finding ->
                adviceCard(
                    finding.appName,
                    "Unused ${lastUsedText(finding.lastUsed).lowercase()} but can still access ${finding.permissions.distinct().joinToString(", ")}. Review whether it still needs this access.",
                    "Review app"
                ) { showFindingDetail(finding) }
            }
        }

        sectionHeading("Your controls", "Transparent by design")
        miniFeature("On-device", "No account, cloud upload, tracking, or scan history.")
        miniFeature("You choose files", "Choose documents or a folder in Android's file picker. GhostAccess cannot browse private app data.")
    }

    private fun heroCard(score: Int?, result: ScanResult?): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(22), dp(22), dp(22))
        setBackgroundResource(R.drawable.bg_hero)
        addView(text("PRIVACY PULSE", 11, R.color.hero_muted, true).letterSpacing(0.12f))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(
                    when {
                        !hasUsageAccess() -> "Your check needs permission"
                        result == null -> "Ready when you are"
                        result.findings.isEmpty() -> "Looking tidy"
                        else -> "A few things need attention"
                    },
                    27,
                    R.color.white,
                    true
                ).margins(top = 8))
                addView(text(
                    when {
                        !hasUsageAccess() -> "Android keeps you in control."
                        result == null -> "Run a private on-device scan."
                        result.findings.isEmpty() -> "No stale sensitive access found."
                        else -> "${result.findings.size} app${plural(result.findings.size)} worth reviewing."
                    },
                    15,
                    R.color.hero_muted
                ).margins(top = 7))
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(TextView(this@MainActivity).apply {
                gravity = Gravity.CENTER
                text = score?.toString() ?: "—"
                textSize = 30f
                setTextColor(Color.WHITE)
                setTypeface(typeface, 1)
                setBackgroundResource(if ((score ?: 0) >= 80) R.drawable.bg_score_good else R.drawable.bg_score_attention)
            }, LinearLayout.LayoutParams(dp(76), dp(76)).apply { leftMargin = dp(12) })
        })
        addView(chip(
            if (hasUsageAccess()) "●  ${if (result == null) "PRIVATE MODE" else "LAST CHECK ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}"}" else "○  USAGE ACCESS OFF",
            R.drawable.bg_hero_chip,
            R.color.white
        ).margins(top = 18))
    }

    private fun renderApps() {
        content.addView(text("Permission review", 29, R.color.ink, true).margins(top = 10))
        content.addView(text("Real app data from Android. Tap an app to understand why it appears and review its settings.", 15, R.color.muted).margins(top = 7, bottom = 15))
        if (!hasUsageAccess()) {
            gateCard("Allow Usage Access", "This Android setting lets GhostAccess compare installed apps with their usage history. It does not give access to app content.") { explainUsageAccess() }
            return
        }
        if (latestDeviceResult == null || deviceScanInProgress) {
            gateCard(if (deviceScanInProgress) "Checking your device" else "No device data yet", if (deviceScanInProgress) "Your private scan is running." else "Run a scan to view the installed apps that hold sensitive permissions.") { runDeviceScan() }
            return
        }
        val result = latestDeviceResult ?: return
        content.addView(statStrip(result).margins(bottom = 16))
        content.addView(filterBar().margins(bottom = 14))
        val visible = if (appFilter == AppFilter.REVIEW) result.findings else result.permissionApps
        if (visible.isEmpty()) {
            infoCard("Nothing needs review", "No unused app with sensitive permissions was found in this scan.")
        } else {
            visible.forEach { finding -> content.addView(appCard(finding).margins(bottom = 10)) }
        }
        content.addView(outlineButton("Run a fresh scan") { runDeviceScan() }.margins(top = 6))
    }

    private fun statStrip(result: ScanResult): View = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(statCard(result.appsExamined.toString(), "Apps checked"))
            addView(statCard(result.appsWithSensitivePermissions.toString(), "Sensitive access").margins(left = 10))
            addView(statCard(result.findings.size.toString(), "Need review").margins(left = 10))
        })
    }

    private fun filterBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(filterButton("Review now", appFilter == AppFilter.REVIEW) { appFilter = AppFilter.REVIEW; render() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        addView(filterButton("All permission apps", appFilter == AppFilter.ALL) { appFilter = AppFilter.ALL; render() }.margins(left = 8), LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    private fun renderFiles() {
        content.addView(text("File privacy check", 29, R.color.ink, true).margins(top = 10))
        content.addView(text("Choose exactly what GhostAccess may read. It searches supported text documents locally and never shows or stores matching secret values.", 15, R.color.muted).margins(top = 7, bottom = 18))
        content.addView(fileActionCard("Select documents", "Pick individual files such as .env, .json, .pem, .txt, or .yaml.", "Choose files") { chooseFiles() }.margins(bottom = 10))
        content.addView(fileActionCard("Scan a folder", "Give access to one folder only. Up to 250 supported text files are checked.", "Choose folder") { chooseFolder() }.margins(bottom = 20))
        sectionHeading("What GhostAccess detects", "In files you select")
        miniFeature("Exposed credentials", "AWS access keys, Google API keys, GitHub tokens, and private-key blocks.")
        miniFeature("Protected output", "Only the file name and match type are shown. Secret values are never displayed, retained, or uploaded.")
        if (fileScanInProgress) {
            infoCard("Checking your ${latestFileResult?.scope ?: "selection"}", "The scan is running locally on this phone.")
        } else latestFileResult?.let { result -> renderFileResult(result) }
    }

    private fun renderFileResult(result: FileScanResult) {
        sectionHeading("Latest file check", result.scope.replaceFirstChar { it.uppercase() })
        val detail = buildString {
            append("${result.filesScanned} file${plural(result.filesScanned)} checked")
            if (result.skippedFiles > 0) append(" · ${result.skippedFiles} unreadable")
            if (result.limitReached) append(" · folder limit reached")
        }
        infoCard(if (result.findings.isEmpty()) "No supported key pattern found" else "${result.findings.size} item${plural(result.findings.size)} to review", detail)
        result.findings.forEach { finding ->
            adviceCard("Review this file", finding, "Choose another scan") { chooseFiles() }
        }
    }

    private fun appCard(finding: PermissionFinding): View = card().apply {
        setOnClickListener { showFindingDetail(finding) }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(monogram(finding.appName), LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(finding.appName, 17, R.color.ink, true))
                addView(text(if (finding.isStale) "Review recommended · ${lastUsedText(finding.lastUsed)}" else "Recently used · ${lastUsedText(finding.lastUsed)}", 13, R.color.muted).margins(top = 3))
            }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(12) })
            addView(chip(if (finding.isStale) "REVIEW" else "ACTIVE", if (finding.isStale) R.drawable.bg_chip_attention else R.drawable.bg_chip_good, if (finding.isStale) R.color.orange else R.color.green))
        })
        addView(text(finding.permissions.distinct().joinToString("  ·  "), 14, R.color.ink).margins(top = 14))
        addView(text("Tap for advice and Android settings  ›", 13, R.color.blue, true).margins(top = 12))
    }

    private fun showFindingDetail(finding: PermissionFinding) {
        AlertDialog.Builder(this)
            .setTitle(finding.appName)
            .setMessage("${if (finding.isStale) "This app has not been used recently" else "This app is still in recent use"}, and Android reports it has access to ${finding.permissions.distinct().joinToString(", ")}.\n\nAdvice: only keep these permissions if they are needed for a feature you actively use. Android Settings is where you can change or remove them.")
            .setNegativeButton("Close", null)
            .setPositiveButton("Review settings") { _, _ -> openAppSettings(finding.packageName) }
            .show()
    }

    private fun runDeviceScan() {
        if (!hasUsageAccess()) { explainUsageAccess(); return }
        deviceScanInProgress = true
        render()
        worker.execute {
            try {
                val result = DeviceScanner(this).scan()
                runOnUiThread { latestDeviceResult = result; deviceScanInProgress = false; render() }
            } catch (_: Exception) {
                runOnUiThread { deviceScanInProgress = false; showError("The device check could not finish. Confirm Usage Access, then try again.") }
            }
        }
    }

    private fun chooseFiles() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, FILE_PICKER_REQUEST)
    }

    private fun chooseFolder() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, FOLDER_PICKER_REQUEST)
    }

    private fun scanSelectedFiles(uris: List<Uri>) {
        screen = Screen.FILES
        fileScanInProgress = true
        render()
        worker.execute { runOnUiThread { } ; val result = SecretScanner(contentResolver).scan(uris); runOnUiThread { latestFileResult = result; fileScanInProgress = false; render() } }
    }

    private fun scanFolder(uri: Uri) {
        screen = Screen.FILES
        fileScanInProgress = true
        render()
        worker.execute {
            try {
                val result = SecretScanner(contentResolver).scanFolder(uri)
                runOnUiThread { latestFileResult = result; fileScanInProgress = false; render() }
            } catch (_: Exception) {
                runOnUiThread { fileScanInProgress = false; showError("The folder could not be checked. Choose the folder again and try once more.") }
            }
        }
    }

    private fun explainUsageAccess() {
        AlertDialog.Builder(this)
            .setTitle("Allow a real device check?")
            .setMessage("GhostAccess uses Android Usage Access to find apps you have not used recently that still hold sensitive permissions. Android opens its own Settings screen, and you can decline or revoke access any time.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Open Settings") { _, _ -> waitingForUsageAccess = true; startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("What GhostAccess can check")
            .setMessage("It checks Android-provided installed-app metadata, granted sensitive permissions, and usage summaries. It can scan files and folders you deliberately select.\n\nIt cannot read another app's private files, passwords, screen contents, or API keys stored inside other apps. All results stay on this device.")
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this).setTitle("Try again").setMessage(message).setPositiveButton("OK", null).show()
        render()
    }

    private fun openAppSettings(packageName: String) = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun updateNav() {
        listOf(homeTab to Screen.HOME, appsTab to Screen.APPS, filesTab to Screen.FILES).forEach { (button, target) ->
            button.setTextColor(color(if (screen == target) R.color.blue else R.color.muted))
            button.setBackgroundResource(if (screen == target) R.drawable.bg_nav_active else android.R.color.transparent)
        }
    }

    private fun sectionHeading(title: String, subtitle: String) {
        content.addView(text(title, 21, R.color.ink, true).margins(top = 8))
        content.addView(text(subtitle, 13, R.color.muted).margins(top = 3, bottom = 10))
    }

    private fun gateCard(title: String, message: String, action: () -> Unit) {
        content.addView(card().apply {
            addView(text(title, 20, R.color.ink, true))
            addView(text(message, 15, R.color.muted).margins(top = 7))
            addView(primaryButton(if (deviceScanInProgress) "Checking…" else "Continue").apply { isEnabled = !deviceScanInProgress; setOnClickListener { action() } }.margins(top = 16))
        })
    }

    private fun adviceCard(title: String, message: String, actionLabel: String, action: () -> Unit) {
        content.addView(card().apply {
            addView(text(title, 17, R.color.ink, true))
            addView(text(message, 14, R.color.muted).margins(top = 6))
            addView(text(actionLabel + "  ›", 14, R.color.blue, true).apply { setPadding(0, dp(13), 0, 0); setOnClickListener { action() } })
        }.margins(bottom = 10))
    }

    private fun infoCard(title: String, message: String) {
        content.addView(card().apply {
            addView(text(title, 17, R.color.ink, true))
            addView(text(message, 14, R.color.muted).margins(top = 6))
        }.margins(bottom = 10))
    }

    private fun miniFeature(title: String, message: String) {
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
            addView(TextView(this@MainActivity).apply { text = "✦"; textSize = 17f; setTextColor(color(R.color.blue)) }, LinearLayout.LayoutParams(dp(28), WRAP))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(title, 15, R.color.ink, true))
                addView(text(message, 13, R.color.muted).margins(top = 3))
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
        })
    }

    private fun fileActionCard(title: String, message: String, actionLabel: String, action: () -> Unit): View = card().apply {
        addView(text(title, 18, R.color.ink, true))
        addView(text(message, 14, R.color.muted).margins(top = 5))
        addView(outlineButton(actionLabel, action).margins(top = 14))
    }

    private fun statCard(value: String, label: String): View = card().apply {
        layoutParams = LinearLayout.LayoutParams(dp(140), WRAP)
        addView(text(value, 27, R.color.ink, true))
        addView(text(label, 13, R.color.muted).margins(top = 3))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundResource(R.drawable.bg_card)
    }

    private fun primaryButton(label: String): Button = button(label, true)
    private fun outlineButton(label: String, action: () -> Unit): Button = button(label, false).apply { setOnClickListener { action() } }
    private fun filterButton(label: String, selected: Boolean, action: () -> Unit): Button = button(label, selected).apply { setOnClickListener { action() }; textSize = 13f }
    private fun iconButton(label: String, description: String, action: () -> Unit): Button = button(label, false).apply { contentDescription = description; textSize = 19f; setPadding(0, 0, 0, 0); setOnClickListener { action() } }
    private fun navButton(label: String, action: () -> Unit): Button = Button(this).apply { text = label; textSize = 12f; setAllCaps(false); gravity = Gravity.CENTER; setPadding(0, 0, 0, 0); setOnClickListener { action() } }
    private fun chip(label: String, background: Int, textColor: Int): TextView = text(label, 11, textColor, true).apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6)); setBackgroundResource(background); layoutParams = LinearLayout.LayoutParams(WRAP, WRAP) }
    private fun monogram(name: String): TextView = TextView(this).apply { text = name.firstOrNull()?.uppercase() ?: "•"; gravity = Gravity.CENTER; textSize = 17f; setTextColor(color(R.color.blue)); setTypeface(typeface, 1); setBackgroundResource(R.drawable.bg_monogram) }

    private fun button(label: String, primary: Boolean): Button = Button(this).apply {
        text = label; setAllCaps(false); textSize = 15f; minHeight = 0; gravity = Gravity.CENTER
        setTextColor(if (primary) Color.WHITE else color(R.color.blue))
        setBackgroundResource(if (primary) R.drawable.bg_primary_button else R.drawable.bg_secondary_button)
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun text(value: String, size: Int, colorId: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color(colorId)); if (bold) setTypeface(typeface, 1); includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun TextView.letterSpacing(value: Float): TextView = apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) letterSpacing = value }
    private fun View.margins(top: Int = 0, bottom: Int = 0, left: Int = 0): View { if (layoutParams == null) layoutParams = LinearLayout.LayoutParams(MATCH, WRAP); (layoutParams as? ViewGroup.MarginLayoutParams)?.apply { topMargin = dp(top); bottomMargin = dp(bottom); leftMargin = dp(left) }; return this }
    private fun color(id: Int) = getColor(id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun plural(value: Int) = if (value == 1) "" else "s"
    private fun lastUsedText(lastUsed: Long): String = if (lastUsed == 0L) "No recorded use in the past year" else "Last used about ${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUsed)} day${plural(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUsed).toInt())} ago"

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val FILE_PICKER_REQUEST = 404
        const val FOLDER_PICKER_REQUEST = 405
    }
}
