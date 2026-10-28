package com.ghostaccess.scanner

import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.util.ArrayDeque

data class StorageAuditResult(
    val entriesInspected: Int,
    val contentFilesScanned: Int,
    val findings: List<String>,
    val unreadableFiles: Int,
    val limitedFiles: Int,
    val restrictedDirectoriesSkipped: Int
)

/**
 * Audits shared storage after the owner explicitly enables Android's all-files special access.
 * Android deliberately blocks other apps' private storage; this scanner never attempts to bypass that boundary.
 */
class StorageScanner {
    fun scanSharedStorage(): StorageAuditResult {
        val pending = ArrayDeque<File>()
        pending.add(Environment.getExternalStorageDirectory())
        var entries = 0
        var contentScanned = 0
        var unreadable = 0
        var limited = 0
        var privateDirectories = 0
        val findings = mutableListOf<String>()

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            val children = current.listFiles()
            if (children == null) {
                unreadable++
                continue
            }
            children.forEach { file ->
                entries++
                if (file.isDirectory) {
                    if (isRestrictedAppDirectory(file)) privateDirectories++ else pending.add(file)
                } else if (file.isFile && file.canRead() && isLikelyTextOrConfig(file.name)) {
                    try {
                        val text = readAtMost(file)
                        contentScanned++
                        patterns.forEach { pattern ->
                            if (pattern.regex.containsMatchIn(text)) findings += "${pattern.label} in ${displayPath(file)}"
                        }
                        if (file.length() > MAX_BYTES_PER_FILE) limited++
                    } catch (_: Exception) {
                        unreadable++
                    }
                }
            }
        }
        return StorageAuditResult(
            entriesInspected = entries,
            contentFilesScanned = contentScanned,
            findings = findings.distinct(),
            unreadableFiles = unreadable,
            limitedFiles = limited,
            restrictedDirectoriesSkipped = privateDirectories
        )
    }

    private fun isRestrictedAppDirectory(file: File): Boolean {
        val normalized = file.absolutePath.replace('\\', '/')
        return normalized.endsWith("/Android/data") || normalized.endsWith("/Android/obb")
    }

    private fun displayPath(file: File): String = file.absolutePath
        .removePrefix(Environment.getExternalStorageDirectory().absolutePath)
        .ifBlank { file.name }
        .takeLast(100)

    private fun isLikelyTextOrConfig(name: String): Boolean = name.lowercase().let { filename ->
        TEXT_EXTENSIONS.any(filename::endsWith)
    }

    private fun readAtMost(file: File): String = FileInputStream(file).use { input ->
        val buffer = ByteArray(MAX_BYTES_PER_FILE)
        val count = input.read(buffer)
        if (count <= 0) "" else String(buffer, 0, count, Charsets.UTF_8)
    }

    private data class SecretPattern(val label: String, val regex: Regex)

    private companion object {
        const val MAX_BYTES_PER_FILE = 3 * 1024 * 1024
        val TEXT_EXTENSIONS = listOf(
            ".env", ".json", ".yaml", ".yml", ".xml", ".properties", ".conf", ".ini",
            ".md", ".txt", ".csv", ".pem", ".key", ".gradle", ".toml", ".js", ".ts",
            ".kt", ".java", ".py", ".rb", ".php", ".sh", ".sql", ".html", ".css"
        )
        val patterns = listOf(
            SecretPattern("Possible AWS access key", Regex("\\bAKIA[0-9A-Z]{16}\\b")),
            SecretPattern("Possible Google API key", Regex("\\bAIza[0-9A-Za-z_-]{35}\\b")),
            SecretPattern("Possible GitHub token", Regex("\\bgh[pousr]_[A-Za-z0-9_]{20,255}\\b")),
            SecretPattern("Possible private key block", Regex("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"))
        )
    }
}
