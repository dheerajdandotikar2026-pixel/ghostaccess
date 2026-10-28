package com.ghostaccess.scanner

import android.content.ContentResolver
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

data class FileScanResult(
    val filesScanned: Int,
    val findings: List<String>,
    val skippedFiles: Int
)

/** Scans only documents chosen by the user and keeps neither their contents nor secret values. */
class SecretScanner(private val contentResolver: ContentResolver) {
    fun scan(uris: List<Uri>): FileScanResult {
        var scanned = 0
        var skipped = 0
        val findings = mutableListOf<String>()
        uris.forEach { uri ->
            try {
                val text = contentResolver.openInputStream(uri)?.use { input ->
                    readAtMost(BufferedReader(InputStreamReader(input)), MAX_CHARS)
                } ?: return@forEach
                scanned++
                val name = documentName(uri)
                patterns.forEach { pattern ->
                    if (pattern.regex.containsMatchIn(text)) {
                        findings += "${pattern.label} in $name"
                    }
                }
            } catch (_: Exception) {
                skipped++
            }
        }
        return FileScanResult(scanned, findings.distinct(), skipped)
    }

    private fun documentName(uri: Uri): String = uri.lastPathSegment?.takeLast(70) ?: "selected document"

    private fun readAtMost(reader: BufferedReader, maximumCharacters: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(8_192)
        while (output.length < maximumCharacters) {
            val remaining = maximumCharacters - output.length
            val read = reader.read(buffer, 0, minOf(buffer.size, remaining))
            if (read == -1) break
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private data class SecretPattern(val label: String, val regex: Regex)

    private companion object {
        const val MAX_CHARS = 2_000_000
        val patterns = listOf(
            SecretPattern("Possible AWS access key", Regex("\\bAKIA[0-9A-Z]{16}\\b")),
            SecretPattern("Possible Google API key", Regex("\\bAIza[0-9A-Za-z_-]{35}\\b")),
            SecretPattern("Possible GitHub token", Regex("\\bgh[pousr]_[A-Za-z0-9_]{20,255}\\b")),
            SecretPattern("Possible private key block", Regex("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"))
        )
    }
}
