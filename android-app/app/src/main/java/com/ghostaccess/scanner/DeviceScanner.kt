package com.ghostaccess.scanner

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.concurrent.TimeUnit

data class SecuritySignal(val title: String, val detail: String, val weight: Int)

data class PermissionFinding(
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val lastUsed: Long,
    val riskScore: Int,
    val isStale: Boolean,
    val signals: List<SecuritySignal>,
    val targetSdk: Int,
    val installer: String?,
    val isUserInstalled: Boolean
)

data class ScanResult(
    val appsExamined: Int,
    val appsWithSensitivePermissions: Int,
    val findings: List<PermissionFinding>,
    val permissionApps: List<PermissionFinding>,
    val inspectedApps: List<PermissionFinding>
)

/** Reads Android package metadata, permissions, installer source and usage summaries only. */
class DeviceScanner(private val context: Context) {
    private val packageManager = context.packageManager
    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    fun scan(now: Long = System.currentTimeMillis()): ScanResult {
        val usageByPackage = usageStats
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - YEAR_MILLIS, now)
            .orEmpty()
            .associateBy { it.packageName }
        val packages = installedPackages()
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
        val inspected = packages.map { info ->
            inspect(info, usageByPackage[info.packageName]?.lastTimeUsed ?: 0L, now)
        }.sortedWith(compareByDescending<PermissionFinding> { it.riskScore }.thenBy { it.appName.lowercase() })
        val permissionApps = inspected.filter { it.permissions.isNotEmpty() || it.signals.isNotEmpty() }
        return ScanResult(
            appsExamined = inspected.size,
            appsWithSensitivePermissions = inspected.count { it.permissions.isNotEmpty() },
            findings = permissionApps.filter { it.isStale || it.signals.isNotEmpty() },
            permissionApps = permissionApps,
            inspectedApps = inspected
        )
    }

    private fun inspect(packageInfo: PackageInfo, lastUsed: Long, now: Long): PermissionFinding {
        val appInfo = packageInfo.applicationInfo
        val grantedSensitive = grantedSensitivePermissions(packageInfo)
        val isUserInstalled = appInfo != null && appInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
        val isStale = lastUsed == 0L || now - lastUsed >= STALE_AFTER_MILLIS
        val signals = mutableListOf<SecuritySignal>()
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        if (grantedSensitive.isNotEmpty() && isStale) {
            signals += SecuritySignal("Inactive with sensitive access", "Not used recently but still has ${grantedSensitive.size} sensitive permission${plural(grantedSensitive.size)}.", 3)
        }
        HIGH_RISK_DECLARATIONS.forEach { (permission, signal) -> if (permission in requested) signals += signal }
        if (appInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            signals += SecuritySignal("Debug build", "Android marks this app as debuggable.", 2)
        }
        if (isUserInstalled && (appInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT) < 26) {
            signals += SecuritySignal("Older Android target", "Targets an Android version from before modern permission safeguards.", 1)
        }
        val installer = installerFor(packageInfo.packageName)
        if (isUserInstalled && installer.isNullOrBlank()) {
            signals += SecuritySignal("Installer not reported", "Android did not report where this user-installed app came from.", 1)
        }
        return PermissionFinding(
            packageName = packageInfo.packageName,
            appName = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageInfo.packageName,
            permissions = grantedSensitive.map(::readablePermission),
            lastUsed = lastUsed,
            riskScore = grantedSensitive.sumOf(::permissionWeight) + signals.sumOf { it.weight },
            isStale = isStale,
            signals = signals,
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            installer = installer,
            isUserInstalled = isUserInstalled
        )
    }

    @Suppress("DEPRECATION")
    private fun installerFor(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) packageManager.getInstallSourceInfo(packageName).installingPackageName
        else packageManager.getInstallerPackageName(packageName)
    } catch (_: Exception) { null }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
    } else packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

    private fun grantedSensitivePermissions(packageInfo: PackageInfo): List<String> {
        val requested = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(requested.size)
        return requested.filterIndexed { index, permission ->
            permission in SENSITIVE_PERMISSIONS && flags.getOrElse(index) { 0 }.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        }
    }

    private fun plural(value: Int) = if (value == 1) "" else "s"

    companion object {
        private val STALE_AFTER_MILLIS = TimeUnit.DAYS.toMillis(90)
        private val YEAR_MILLIS = TimeUnit.DAYS.toMillis(365)
        private val SENSITIVE_PERMISSIONS = setOf(
            "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR",
            "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG", "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
            "android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS", "android.permission.POST_NOTIFICATIONS",
            "android.permission.BODY_SENSORS", "android.permission.ACTIVITY_RECOGNITION"
        )
        private val HIGH_RISK_DECLARATIONS = mapOf(
            "android.permission.SYSTEM_ALERT_WINDOW" to SecuritySignal("Can appear over other apps", "Declared overlay access, which can obscure what you tap.", 2),
            "android.permission.REQUEST_INSTALL_PACKAGES" to SecuritySignal("Can request app installs", "Declared the ability to hand off APK installs to Android.", 2),
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to SecuritySignal("Accessibility service declared", "Accessibility services can observe and assist with on-screen actions.", 2),
            "android.permission.MANAGE_EXTERNAL_STORAGE" to SecuritySignal("Broad shared-file access declared", "Declared Android's special all-files access.", 2),
            "android.permission.PACKAGE_USAGE_STATS" to SecuritySignal("Usage access declared", "Declared access to app usage summaries.", 1),
            "android.permission.RECEIVE_BOOT_COMPLETED" to SecuritySignal("Starts after reboot", "Declared the ability to receive device boot events.", 1)
        )
        private fun readablePermission(permission: String): String = when (permission) {
            "android.permission.CAMERA" -> "Camera"
            "android.permission.RECORD_AUDIO" -> "Microphone"
            "android.permission.ACCESS_FINE_LOCATION" -> "Precise location"
            "android.permission.ACCESS_COARSE_LOCATION" -> "Approximate location"
            "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS" -> "Contacts"
            "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR" -> "Calendar"
            "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG" -> "Call history"
            "android.permission.READ_PHONE_STATE" -> "Phone status"
            "android.permission.CALL_PHONE" -> "Make calls"
            "android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS" -> "SMS"
            "android.permission.POST_NOTIFICATIONS" -> "Notifications"
            "android.permission.BODY_SENSORS" -> "Body sensors"
            "android.permission.ACTIVITY_RECOGNITION" -> "Physical activity"
            else -> permission.substringAfterLast('.')
        }
        private fun permissionWeight(permission: String): Int = when (permission) {
            "android.permission.RECORD_AUDIO", "android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS" -> 3
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG" -> 2
            else -> 1
        }
    }
}
