package dev.subfly.yaba.core.filesystem.access

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.filesystem.DocmarkFileManager
import dev.subfly.yaba.core.filesystem.ImagemarkFileManager
import dev.subfly.yaba.core.filesystem.YabaFile
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.model.utils.DocmarkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Centralized accessor for Android document pickers, camera, saver, share, and open-with-default.
 *
 * Call [register] once per [ComponentActivity] from [ComponentActivity.onCreate] (after
 * [android.os.Bundle] super call) before using suspend picker APIs.
 *
 * This is separate from [FileAccessProvider], which manages app-private storage under the YABA
 * working directory.
 */
object YabaFileAccessor {
    private var hostActivity: ComponentActivity? = null

    private var pending: ((Any?) -> Unit)? = null

    private var pickImageSingle: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pickImageMultiple: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var openDocument: ActivityResultLauncher<Array<String>>? = null
    private var openMultipleDocuments: ActivityResultLauncher<Array<String>>? = null
    private var openTree: ActivityResultLauncher<Uri?>? = null
    private var createDocument: ActivityResultLauncher<Intent>? = null
    private var requestCameraPermission: ActivityResultLauncher<String>? = null
    private var takePicture: ActivityResultLauncher<Uri>? = null

    private var cameraOutputFile: File? = null

    /**
     * Registers activity-result contracts. Must be called from each activity instance's [ComponentActivity.onCreate].
     */
    fun register(activity: ComponentActivity) {
        if (hostActivity === activity) return
        hostActivity = activity

        pickImageSingle = activity.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri -> completePending(uri) }

        pickImageMultiple = activity.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(50),
        ) { uris -> completePending(uris) }

        openDocument = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri -> completePending(uri) }

        openMultipleDocuments = activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris -> completePending(uris) }

        openTree = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri -> completePending(uri) }

        createDocument = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val uri =
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data
                } else {
                    null
                }
            completePending(uri)
        }

        requestCameraPermission = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            completePending(granted)
        }

        takePicture = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            val file = cameraOutputFile
            val act = hostActivity
            val result =
                if (success && file != null && file.exists() && act != null) {
                    YabaFile.fromFile(act, file)
                } else {
                    null
                }
            cameraOutputFile = null
            completePending(result)
        }
    }

    private fun completePending(result: Any?) {
        val cb = pending
        pending = null
        cb?.invoke(result)
    }

    // region Image pickers

    suspend fun pickSingleImage(): YabaFile? {
        val act = hostActivity ?: return null
        val launcher = pickImageSingle ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uri = result as? Uri
                    cont.resume(uri?.let { YabaFile.fromUri(act, it) })
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            }
        }
    }

    suspend fun pickMultipleImages(maxItems: Int? = null): List<YabaFile>? {
        val act = hostActivity ?: return null
        val launcher = pickImageMultiple ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uris = result as? List<Uri> ?: emptyList()
                    val mapped = uris.map { YabaFile.fromUri(act, it) }
                    val cap = maxItems ?: 50
                    val limited = mapped.take(cap)
                    cont.resume(limited)
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            }
        }
    }

    suspend fun capturePhoto(): YabaFile? {
        val act = hostActivity ?: return null
        val launcher = takePicture ?: return null
        val cameraPermissionGranted = ensureCameraPermission()
        if (!cameraPermissionGranted) return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val out = File(act.cacheDir, "yaba_capture_${System.currentTimeMillis()}.jpg")
                cameraOutputFile = out
                val uri = FileProvider.getUriForFile(
                    act,
                    "${act.packageName}.fileprovider",
                    out,
                )
                val callback: (Any?) -> Unit = { result ->
                    cont.resume(result as? YabaFile)
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                try {
                    launcher.launch(uri)
                } catch (_: Exception) {
                    if (pending === callback) pending = null
                    cameraOutputFile = null
                    cont.resume(null)
                }
            }
        }
    }

    // endregion

    // region Generic pickers

    suspend fun pickSingleFile(extensions: List<String>? = null): YabaFile? {
        val act = hostActivity ?: return null
        val launcher = openDocument ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uri = result as? Uri
                    cont.resume(uri?.let { YabaFile.fromUri(act, it) })
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(mimeTypesForExtensions(extensions))
            }
        }
    }

    suspend fun pickMultipleFiles(
        extensions: List<String>? = null,
        maxItems: Int? = null,
    ): List<YabaFile>? {
        val act = hostActivity ?: return null
        val launcher = openMultipleDocuments ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uris = result as? List<Uri> ?: emptyList()
                    val mapped = uris.map { YabaFile.fromUri(act, it) }
                    val limited = if (maxItems != null) mapped.take(maxItems) else mapped
                    cont.resume(limited)
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(mimeTypesForExtensions(extensions))
            }
        }
    }

    suspend fun pickDirectory(): YabaFile? {
        val act = hostActivity ?: return null
        val launcher = openTree ?: return null
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uri = result as? Uri
                    val tree = uri?.let { YabaFile.fromTreeUri(act, it) }
                    cont.resume(tree)
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(null)
            }
        }
    }

    // endregion

    // region Save / export

    suspend fun openFileSaver(
        suggestedName: String,
        extension: String,
        @Suppress("UNUSED_PARAMETER") directory: YabaFile? = null,
    ): YabaFile? {
        val act = hostActivity ?: return null
        val launcher = createDocument ?: return null
        val mime = mimeForExtension(extension)
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    val uri = result as? Uri
                    cont.resume(uri?.let { YabaFile.fromUri(act, it) })
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                val filename =
                    if (suggestedName.endsWith(".$extension", ignoreCase = true)) {
                        suggestedName
                    } else {
                        "$suggestedName.$extension"
                    }
                val intent =
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = mime
                        putExtra(Intent.EXTRA_TITLE, filename)
                    }
                launcher.launch(intent)
            }
        }
    }

    suspend fun saveFileCopy(
        bytes: ByteArray,
        suggestedName: String,
        extension: String,
    ): Boolean {
        val file = openFileSaver(suggestedName, extension) ?: return false
        return runCatching { withContext(Dispatchers.IO) { file.write(bytes) } }.isSuccess
    }

    suspend fun saveFileCopy(
        sourceFile: YabaFile,
        suggestedName: String,
        extension: String,
    ): Boolean {
        val targetFile = openFileSaver(suggestedName, extension) ?: return false
        return runCatching {
            withContext(Dispatchers.IO) {
                val bytes = sourceFile.readBytes()
                targetFile.write(bytes)
            }
        }.isSuccess
    }

    suspend fun exportNotemarkMarkdownBundle(
        markdown: String,
        bookmarkId: String,
        suggestedMarkdownBaseName: String,
    ): Boolean {
        if (markdown.isBlank()) return false
        val directory = pickDirectory() ?: return false
        return writeNotemarkExportToDirectory(
            directory = directory,
            markdown = markdown,
            bookmarkId = bookmarkId,
            suggestedMarkdownBaseName = suggestedMarkdownBaseName,
        )
    }

    internal suspend fun writeNotemarkExportToDirectory(
        directory: YabaFile,
        markdown: String,
        bookmarkId: String,
        suggestedMarkdownBaseName: String,
    ): Boolean {
        if (markdown.isBlank()) return false
        val base = suggestedMarkdownBaseName.ifBlank { "note" }
        val result = runCatching {
            val assetsRel = CoreConstants.FileSystem.Linkmark.assetsDir(bookmarkId)
            val sourceAssets = BookmarkFileManager.resolve(assetsRel, ensureParentExists = false)
            val exportRoot = directory / base
            exportRoot.createDirectories()
            val assetsDir = exportRoot / "assets"
            assetsDir.createDirectories()
            if (sourceAssets.exists() && sourceAssets.isDirectory()) {
                for (child in sourceAssets.list()) {
                    if (child.isDirectory().not()) {
                        child.copyTo(assetsDir / child.name)
                    }
                }
            }
            val mdFile = exportRoot / "note.md"
            mdFile.write(markdown.encodeToByteArray())
        }.onFailure { e ->
            e.printStackTrace()
        }
        return result.isSuccess
    }

    // endregion

    // region Share

    suspend fun shareImageBookmark(bookmarkId: String) {
        val originalRel = DatabaseProvider.imageBookmarkDao.getByBookmarkId(bookmarkId)?.originalImageRelativePath
        val file = ImagemarkFileManager.getShareableImageFile(
            bookmarkId = bookmarkId,
            originalRelativePath = originalRel,
        ) ?: return
        shareFileInternal(file)
    }

    suspend fun exportImageBookmark(
        bookmarkId: String,
        suggestedName: String,
        extension: String,
    ): Boolean {
        val originalRel = DatabaseProvider.imageBookmarkDao.getByBookmarkId(bookmarkId)?.originalImageRelativePath
        val file = ImagemarkFileManager.getShareableImageFile(
            bookmarkId = bookmarkId,
            originalRelativePath = originalRel,
        ) ?: return false
        return saveFileCopy(file, suggestedName, extension)
    }

    suspend fun shareDocmark(bookmarkId: String) {
        val type =
            DatabaseProvider.docBookmarkDao.getByBookmarkId(bookmarkId)?.type ?: DocmarkType.PDF
        val file = DocmarkFileManager.getDocumentFile(bookmarkId, type) ?: return
        shareFileInternal(file)
    }

    suspend fun exportDocmark(
        bookmarkId: String,
        suggestedName: String,
        extension: String? = null,
    ): Boolean {
        val type =
            DatabaseProvider.docBookmarkDao.getByBookmarkId(bookmarkId)?.type ?: DocmarkType.PDF
        val file = DocmarkFileManager.getDocumentFile(bookmarkId, type) ?: return false
        val ext = extension ?: DocmarkFileManager.extensionForType(type)
        return saveFileCopy(file, suggestedName, ext)
    }

    internal fun shareFile(file: YabaFile) {
        shareFileInternal(file)
    }

    fun shareFiles(files: List<YabaFile>) {
        val act = hostActivity ?: return
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFileInternal(files.first())
            return
        }
        val uris = files.mapNotNull { it.toShareableUri(act) }
        if (uris.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        act.startActivity(Intent.createChooser(intent, null))
    }

    // endregion

    // region Open with default app

    fun openFile(file: YabaFile) {
        val act = hostActivity ?: return
        val uri = file.toShareableUri(act) ?: return
        val mime = contentResolverMime(act, uri, file.extension)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        act.startActivity(Intent.createChooser(intent, null))
    }

    // endregion

    private fun shareFileInternal(file: YabaFile) {
        val act = hostActivity ?: return
        val uri = file.toShareableUri(act) ?: return
        val mime = contentResolverMime(act, uri, file.extension)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        act.startActivity(Intent.createChooser(intent, null))
    }

    private fun contentResolverMime(
        context: ComponentActivity,
        uri: Uri,
        extFallback: String,
    ): String {
        val fromResolver = context.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank()) return fromResolver
        val map = MimeTypeMap.getSingleton()
        val mime = map.getMimeTypeFromExtension(extFallback.lowercase().removePrefix("."))
        return mime ?: "*/*"
    }

    private suspend fun ensureCameraPermission(): Boolean {
        val act = hostActivity ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        return withContext(Dispatchers.Main) {
            val granted = ContextCompat.checkSelfPermission(
                act,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) return@withContext true
            val launcher = requestCameraPermission ?: return@withContext false

            suspendCancellableCoroutine { cont ->
                if (pending != null) {
                    cont.resume(false)
                    return@suspendCancellableCoroutine
                }
                val callback: (Any?) -> Unit = { result ->
                    cont.resume(result as? Boolean == true)
                }
                pending = callback
                cont.invokeOnCancellation {
                    if (pending === callback) pending = null
                }
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun mimeTypesForExtensions(extensions: List<String>?): Array<String> {
    if (extensions.isNullOrEmpty()) return arrayOf("*/*")
    return extensions.map { ext ->
        val x = ext.lowercase().removePrefix(".")
        mimeForExtension(x)
    }.distinct().toTypedArray()
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
        "bin" -> "application/octet-stream"
        else -> "application/octet-stream"
    }
