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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : android.app.Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var summary: TextView
    private lateinit var findings: LinearLayout
    private lateinit var scanButton: Button
    private var waitingForUsageAccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = color(R.color.canvas)
        window.navigationBarColor = color(R.color.canvas)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(createScreen())

        if (!hasUsageAccess()) {
            status.post { explainUsageAccess() }
        } else {
            runDeviceScan()
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForUsageAccess) {
            waitingForUsageAccess = false
            if (hasUsageAccess()) runDeviceScan() else showAccessNeeded()
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_PICKER_REQUEST || resultCode != RESULT_OK || data == null) return

        val uris = buildList {
            data.data?.let(::add)
            data.clipData?.let { clip ->
                repeat(clip.itemCount) { index -> add(clip.getItemAt(index).uri) }
            }
        }.distinct()
        if (uris.isNotEmpty()) scanSelectedFiles(uris)
    }

    private fun createScreen(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.canvas))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(36))
        }
        scroll.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))

        content.addView(text("GhostAccess", 34, R.color.ink, true))
        content.addView(text("A private, on-device permission check.", 17, R.color.muted).margins(top = 5, bottom = 24))

        val trustCard = card().apply {
            addView(text("Your data stays on your phone", 18, R.color.ink, true))
            addView(text(
                "GhostAccess reads Android's installed-app metadata and usage summaries. It does not upload results, read other apps' private files, or show your secrets.",
                15, R.color.muted
            ).margins(top = 8))
        }
        content.addView(trustCard.margins(bottom = 16))

        status = text("Usage access is needed before the first scan.", 15, R.color.muted).apply {
            setPadding(0, 0, 0, dp(12))
        }
        content.addView(status)

        scanButton = button("Run device scan", primary = true).apply {
            setOnClickListener {
                if (hasUsageAccess()) runDeviceScan() else explainUsageAccess()
            }
        }
        content.addView(scanButton)

        content.addView(button("Scan selected files for exposed keys", primary = false).apply {
            setOnClickListener { chooseFiles() }
        }.margins(top = 10, bottom = 18))

        summary = text("No device scan has run yet.", 16, R.color.ink, true)
        content.addView(summary.margins(bottom = 10))

        findings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(findings)
        return scroll
    }

    private fun explainUsageAccess() {
        AlertDialog.Builder(this)
            .setTitle("Allow a device permission check?")
            .setMessage(
                "GhostAccess needs Android Usage Access to identify apps that have not been used recently but still hold sensitive permissions. Android will open its own Settings screen, where you can allow or decline it."
            )
            .setNegativeButton("Not now") { _, _ -> showAccessNeeded() }
            .setPositiveButton("Open Settings") { _, _ ->
                waitingForUsageAccess = true
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .show()
    }

    private fun showAccessNeeded() {
        status.text = "Usage access was not granted. You can enable it whenever you are ready."
        summary.text = "A device scan needs your permission."
        findings.removeAllViews()
    }

    private fun runDeviceScan() {
        status.text = "Checking installed apps and granted permissions…"
        scanButton.isEnabled = false
        findings.removeAllViews()
        worker.execute {
            try {
                val result = DeviceScanner(this).scan()
                runOnUiThread { showDeviceResults(result) }
            } catch (_: Exception) {
                runOnUiThread {
                    status.text = "The scan could not finish. Try again after confirming Usage Access."
                    scanButton.isEnabled = true
                }
            }
        }
    }

    private fun showDeviceResults(result: ScanResult) {
        scanButton.isEnabled = true
        status.text = "Last checked ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())} · On-device only"
        summary.text = if (result.findings.isEmpty()) {
            "No unused apps with sensitive permissions found."
        } else {
            "${result.findings.size} app${plural(result.findings.size)} may have permissions worth reviewing."
        }
        findings.removeAllViews()
        addInfoCard("Apps checked", result.appsExamined.toString(), "Apps with one or more sensitive permissions: ${result.appsWithSensitivePermissions}")

        if (result.findings.isEmpty()) {
            addInfoCard("Nothing to review", "No stale sensitive permissions", "This result reflects Android's usage history from the last year.")
            return
        }
        result.findings.forEach { finding -> findings.addView(findingCard(finding).margins(bottom = 10)) }
    }

    private fun findingCard(finding: PermissionFinding): View = card().apply {
        addView(text(finding.appName, 18, R.color.ink, true))
        addView(text(finding.permissions.distinct().joinToString(" · "), 15, R.color.orange, true).margins(top = 6))
        addView(text(lastUsedText(finding.lastUsed), 14, R.color.muted).margins(top = 6))
        addView(button("Review in Android Settings", primary = false).apply {
            setOnClickListener { openAppSettings(finding.packageName) }
        }.margins(top = 12))
    }

    private fun addInfoCard(title: String, value: String, detail: String) {
        findings.addView(card().apply {
            addView(text(title, 14, R.color.muted, true))
            addView(text(value, 21, R.color.ink, true).margins(top = 4))
            addView(text(detail, 14, R.color.muted).margins(top = 6))
        }.margins(bottom = 10))
    }

    private fun chooseFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, FILE_PICKER_REQUEST)
    }

    private fun scanSelectedFiles(uris: List<Uri>) {
        status.text = "Scanning ${uris.size} selected file${plural(uris.size)} locally…"
        worker.execute {
            val fileResult = SecretScanner(contentResolver).scan(uris)
            runOnUiThread { showFileResults(fileResult) }
        }
    }

    private fun showFileResults(result: FileScanResult) {
        status.text = "Selected-file scan complete · Nothing was uploaded or retained."
        findings.removeAllViews()
        summary.text = if (result.findings.isEmpty()) "No supported key pattern found in the selected files." else "${result.findings.size} possible secret${plural(result.findings.size)} found."
        addInfoCard("Files checked", result.filesScanned.toString(), if (result.skippedFiles == 0) "Only documents you selected were read." else "${result.skippedFiles} file${plural(result.skippedFiles)} could not be read.")
        result.findings.forEach { finding ->
            findings.addView(card().apply {
                addView(text("Review this file", 15, R.color.orange, true))
                addView(text(finding, 15, R.color.ink).margins(top = 5))
                addView(text("The matching value is deliberately never displayed or saved.", 13, R.color.muted).margins(top = 7))
            }.margins(bottom = 10))
        }
    }

    private fun openAppSettings(packageName: String) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

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

    private fun lastUsedText(lastUsed: Long): String {
        if (lastUsed == 0L) return "No recorded use in the past year"
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUsed)
        return "Last used about $days day${plural(days.toInt())} ago"
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundResource(R.drawable.bg_card)
    }

    private fun button(label: String, primary: Boolean): Button = Button(this).apply {
        text = label
        setAllCaps(false)
        textSize = 16f
        minHeight = 0
        gravity = Gravity.CENTER
        setTextColor(if (primary) Color.WHITE else color(R.color.blue))
        setBackgroundResource(if (primary) R.drawable.bg_primary_button else R.drawable.bg_secondary_button)
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun text(value: String, size: Int, colorId: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color(colorId))
        if (bold) setTypeface(typeface, 1)
        includeFontPadding = false
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
    }

    private fun View.margins(top: Int = 0, bottom: Int = 0): View {
        if (layoutParams == null) layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
        return this
    }

    private fun color(id: Int) = getColor(id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun plural(value: Int) = if (value == 1) "" else "s"

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val FILE_PICKER_REQUEST = 404
    }
}
