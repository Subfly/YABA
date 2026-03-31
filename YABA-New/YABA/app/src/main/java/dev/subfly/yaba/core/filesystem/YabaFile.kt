package dev.subfly.yaba.core.filesystem

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileNotFoundException

/**
 * Android-native file abstraction replacing KMP PlatformFile: supports app-local [File] paths,
 * Storage Access Framework [DocumentFile] trees/documents, and raw content [Uri] reads.
 */
class YabaFile private constructor(
    private val appContext: Context,
    private val file: File?,
    private val documentFile: DocumentFile?,
    private val contentUri: Uri?,
) {
    constructor(context: Context, file: File) : this(context.applicationContext, file, null, null)

    companion object {
        fun fromFile(context: Context, file: File): YabaFile = YabaFile(context.applicationContext, file, null, null)

        fun fromUri(context: Context, uri: Uri): YabaFile {
            val app = context.applicationContext
            val doc = DocumentFile.fromSingleUri(app, uri)
            return if (doc != null) {
                YabaFile(app, null, doc, null)
            } else {
                YabaFile(app, null, null, uri)
            }
        }

        fun fromTreeUri(context: Context, treeUri: Uri): YabaFile? {
            val doc = DocumentFile.fromTreeUri(context.applicationContext, treeUri) ?: return null
            return YabaFile(context.applicationContext, null, doc, null)
        }

        internal fun fromDocumentFile(context: Context, doc: DocumentFile): YabaFile =
            YabaFile(context.applicationContext, null, doc, null)
    }

    /** Absolute path for local files; URI string for SAF/content. */
    val path: String
        get() = when {
            file != null -> file.absolutePath
            documentFile != null -> documentFile.uri.toString()
            contentUri != null -> contentUri.toString()
            else -> ""
        }

    fun absolutePath(): String = path

    val name: String
        get() = when {
            file != null -> file.name
            documentFile != null -> documentFile.name.orEmpty()
            contentUri != null -> queryDisplayName(contentUri) ?: contentUri.lastPathSegment ?: "file"
            else -> ""
        }

    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    fun exists(): Boolean =
        when {
            file != null -> file.exists()
            documentFile != null -> documentFile.exists()
            contentUri != null -> {
                val uri = contentUri
                runCatching {
                    appContext.contentResolver.openInputStream(uri)?.use { true } ?: false
                }.getOrDefault(false)
            }
            else -> false
        }

    fun isDirectory(): Boolean =
        when {
            file != null -> file.isDirectory
            documentFile != null -> documentFile.isDirectory
            else -> false
        }

    fun list(): List<YabaFile> =
        when {
            file != null && file.isDirectory ->
                file.listFiles()?.map { fromFile(appContext, it) }.orEmpty()
            documentFile != null && documentFile.isDirectory ->
                documentFile.listFiles().map { fromDocumentFile(appContext, it) }
            else -> emptyList()
        }

    fun parent(): YabaFile? =
        when {
            file != null -> file.parentFile?.let { fromFile(appContext, it) }
            documentFile != null -> documentFile.parentFile?.let { fromDocumentFile(appContext, it) }
            else -> null
        }

    fun readBytes(): ByteArray =
        when {
            file != null -> file.readBytes()
            documentFile != null ->
                appContext.contentResolver.openInputStream(documentFile.uri)?.use { it.readBytes() }
                    ?: throw FileNotFoundException(documentFile.uri.toString())
            contentUri != null -> {
                val uri = contentUri
                appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw FileNotFoundException(uri.toString())
            }
            else -> throw IllegalStateException("YabaFile has no readable source")
        }

    fun write(bytes: ByteArray) {
        when {
            file != null -> {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
            }
            documentFile != null -> {
                appContext.contentResolver.openOutputStream(documentFile.uri, "wt")?.use { it.write(bytes) }
                    ?: throw FileNotFoundException(documentFile.uri.toString())
            }
            contentUri != null -> {
                val uri = contentUri
                appContext.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                    ?: throw FileNotFoundException(uri.toString())
            }
            else -> throw IllegalStateException("YabaFile has no writable target")
        }
    }

    fun delete() {
        when {
            file != null -> file.delete()
            documentFile != null -> documentFile.delete()
            else -> { /* content URIs are often not deletable */ }
        }
    }

    fun createDirectories() {
        when {
            file != null -> {
                if (!file.exists() && !file.mkdirs()) {
                    if (!file.exists()) error("Failed to create directories: ${file.absolutePath}")
                }
            }
            documentFile != null -> {
                if (!documentFile.exists()) {
                    error("createDirectories on missing DocumentFile is not supported for SAF roots")
                }
            }
        }
    }

    fun copyTo(target: YabaFile) {
        val bytes = readBytes()
        target.write(bytes)
    }

    /**
     * [Uri] suitable for [android.content.Intent] actions (share, view) with
     * [android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION].
     */
    fun toShareableUri(context: Context): Uri? =
        when {
            file != null ->
                FileProvider.getUriForFile(
                    context.applicationContext,
                    "${context.packageName}.fileprovider",
                    file,
                )
            documentFile != null -> documentFile.uri
            contentUri != null -> contentUri
            else -> null
        }

    fun normalizeDirectory(): YabaFile =
        when {
            file != null ->
                when {
                    file.isDirectory -> this
                    else -> file.parentFile?.let { fromFile(appContext, it) }?.normalizeDirectory() ?: this
                }
            documentFile != null ->
                when {
                    documentFile.isDirectory -> this
                    else -> parent()?.normalizeDirectory() ?: this
                }
            else -> this
        }

    /**
     * Path join: local [File] child, or SAF child (creates directory or file from [segment] name).
     */
    operator fun div(segment: String): YabaFile {
        val trimmed = segment.trim('/')
        require(trimmed.isNotEmpty()) { "Invalid path segment" }
        return when {
            file != null -> fromFile(appContext, File(file, trimmed))
            documentFile != null -> {
                val existing = documentFile.findFile(trimmed)
                if (existing != null) {
                    fromDocumentFile(appContext, existing)
                } else {
                    val looksLikeFile = trimmed.contains('.') && trimmed.lastIndexOf('.') > 0
                    val created =
                        if (looksLikeFile) {
                            val ext = trimmed.substringAfterLast('.')
                            val mime = mimeForExtension(ext)
                            documentFile.createFile(mime, trimmed)
                                ?: documentFile.createFile("application/octet-stream", trimmed)
                        } else {
                            documentFile.createDirectory(trimmed)
                        }
                    fromDocumentFile(appContext, created!!)
                }
            }
            else -> error("Cannot resolve child path on URI-only YabaFile")
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return appContext.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) c.getString(i) else null
            } else null
        }
    }
}

private fun mimeForExtension(ext: String): String =
    when (ext.lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "md" -> "text/markdown"
        "json" -> "application/json"
        "epub" -> "application/epub+zip"
        else -> "application/octet-stream"
    }
