package com.ghostaccess.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayDeque

data class FileScanResult(
    val filesScanned: Int,
    val findings: List<String>,
    val skippedFiles: Int,
    val limitReached: Boolean = false,
    val scope: String = "selected files"
)

/** Reads only documents deliberately selected through Android's system picker. */
class SecretScanner(private val contentResolver: ContentResolver) {
    fun scan(uris: List<Uri>): FileScanResult = scanUris(uris, "selected files")

    fun scanFolder(treeUri: Uri): FileScanResult {
        val documents = mutableListOf<Uri>()
        val folders = ArrayDeque<String>()
        folders.add(DocumentsContract.getTreeDocumentId(treeUri))
        var limitReached = false

        while (folders.isNotEmpty() && documents.size < MAX_FOLDER_FILES) {
            val parentId = folders.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val typeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn) ?: continue
                    val mimeType = cursor.getString(typeColumn).orEmpty()
                    val displayName = cursor.getString(nameColumn).orEmpty()
                    when {
                        mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> folders.add(documentId)
                        isLikelyTextDocument(mimeType, displayName) -> {
                            documents += DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                            if (documents.size == MAX_FOLDER_FILES) limitReached = folders.isNotEmpty()
                        }
                    }
                }
            }
        }
        if (folders.isNotEmpty()) limitReached = true
        return scanUris(documents, "selected folder", limitReached)
    }

    private fun scanUris(uris: List<Uri>, scope: String, limitReached: Boolean = false): FileScanResult {
        var scanned = 0
        var skipped = 0
        val findings = mutableListOf<String>()
        uris.forEach { uri ->
            try {
                val text = contentResolver.openInputStream(uri)?.use { input ->
                    readAtMost(BufferedReader(InputStreamReader(input)), MAX_CHARS_PER_FILE)
                } ?: return@forEach
                scanned++
                val name = documentName(uri)
                patterns.forEach { pattern ->
                    if (pattern.regex.containsMatchIn(text)) findings += "${pattern.label} in $name"
                }
            } catch (_: Exception) {
                skipped++
            }
        }
        return FileScanResult(scanned, findings.distinct(), skipped, limitReached, scope)
    }

    private fun documentName(uri: Uri): String = uri.lastPathSegment?.takeLast(70) ?: "selected document"

    private fun isLikelyTextDocument(mimeType: String, name: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        return name.lowercase().let { filename ->
            listOf(".env", ".json", ".yaml", ".yml", ".xml", ".properties", ".conf", ".ini", ".md", ".txt", ".csv", ".pem", ".key").any(filename::endsWith)
        }
    }

    private fun readAtMost(reader: BufferedReader, maximumCharacters: Int): String {
        val output = StringBuilder()
        val buffer = CharArray(8_192)
        while (output.length < maximumCharacters) {
            val read = reader.read(buffer, 0, minOf(buffer.size, maximumCharacters - output.length))
            if (read == -1) break
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private data class SecretPattern(val label: String, val regex: Regex)

    private companion object {
        const val MAX_CHARS_PER_FILE = 2_000_000
        const val MAX_FOLDER_FILES = 250
        val patterns = listOf(
            SecretPattern("Possible AWS access key", Regex("\\bAKIA[0-9A-Z]{16}\\b")),
            SecretPattern("Possible Google API key", Regex("\\bAIza[0-9A-Za-z_-]{35}\\b")),
            SecretPattern("Possible GitHub token", Regex("\\bgh[pousr]_[A-Za-z0-9_]{20,255}\\b")),
            SecretPattern("Possible private key block", Regex("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"))
        )
    }
}
