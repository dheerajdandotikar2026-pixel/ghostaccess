package com.ghostaccess.scanner

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.concurrent.TimeUnit

data class PermissionFinding(
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val lastUsed: Long,
    val riskScore: Int,
    val isStale: Boolean
)

data class ScanResult(
    val appsExamined: Int,
    val appsWithSensitivePermissions: Int,
    val findings: List<PermissionFinding>,
    val permissionApps: List<PermissionFinding>
)

/**
 * Reads only Android-provided app metadata and usage summaries. It never reads another app's files,
 * network traffic, screen contents, credentials, or private state.
 */
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

        val permissionApps = packages.mapNotNull { packageInfo ->
            val grantedSensitive = grantedSensitivePermissions(packageInfo)
            if (grantedSensitive.isEmpty()) return@mapNotNull null

            val lastUsed = usageByPackage[packageInfo.packageName]?.lastTimeUsed ?: 0L
            val isStale = lastUsed == 0L || now - lastUsed >= STALE_AFTER_MILLIS

            PermissionFinding(
                packageName = packageInfo.packageName,
                appName = packageInfo.applicationInfo?.let { applicationInfo ->
                    packageManager.getApplicationLabel(applicationInfo).toString()
                } ?: packageInfo.packageName,
                permissions = grantedSensitive.map { readablePermission(it) },
                lastUsed = lastUsed,
                riskScore = grantedSensitive.sumOf { permissionWeight(it) },
                isStale = isStale
            )
        }.sortedWith(compareByDescending<PermissionFinding> { it.riskScore }.thenBy { it.appName.lowercase() })
        val findings = permissionApps.filter { it.isStale }

        return ScanResult(
            appsExamined = packages.size,
            appsWithSensitivePermissions = permissionApps.size,
            findings = findings,
            permissionApps = permissionApps
        )
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
    } else {
        packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

    private fun grantedSensitivePermissions(packageInfo: PackageInfo): List<String> {
        val requested = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(requested.size)
        return requested.filterIndexed { index, permission ->
            permission in SENSITIVE_PERMISSIONS &&
                flags.getOrElse(index) { 0 }.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        }
    }

    companion object {
        private val STALE_AFTER_MILLIS = TimeUnit.DAYS.toMillis(90)
        private val YEAR_MILLIS = TimeUnit.DAYS.toMillis(365)

        private val SENSITIVE_PERMISSIONS = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.BODY_SENSORS",
            "android.permission.ACTIVITY_RECOGNITION"
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
            "android.permission.RECORD_AUDIO", "android.permission.CAMERA",
            "android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS" -> 3
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS",
            "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG" -> 2
            else -> 1
        }
    }
}
