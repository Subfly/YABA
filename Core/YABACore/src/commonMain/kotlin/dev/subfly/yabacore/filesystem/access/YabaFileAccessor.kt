package dev.subfly.yabacore.filesystem.access

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.filesystem.DocmarkFileManager
import dev.subfly.yabacore.filesystem.ImagemarkFileManager
import dev.subfly.yabacore.filesystem.NotemarkMarkdownExportBundleDto
import dev.subfly.yabacore.model.utils.DocmarkType
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Centralized accessor for FileKit dialogs and file actions.
 *
 * Exposes picker, camera, saver, share, and open-with-default-app functionality
 * for use across the app. Dialog titles are left empty per product preference.
 *
 * This is separate from [FileAccessProvider], which manages app-private storage
 * under the YABA working directory.
 */
object YabaFileAccessor {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val emptyTitleSettings: FileKitDialogSettings
        get() = FileKitDialogSettings.createDefault()

    // ==================== Image pickers ====================

    /**
     * Opens the file/gallery picker for a single image.
     * On Android/iOS with [FileKitType.Image], uses the native gallery picker.
     *
     * @return The selected [PlatformFile], or null if cancelled.
     */
    suspend fun pickSingleImage(): PlatformFile? = FileKit.openFilePicker(
        type = FileKitType.Image,
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    /**
     * Opens the file/gallery picker for multiple images.
     *
     * @param maxItems Maximum number of images to select (1..50). Null means no limit.
     * @return The selected files, or null if cancelled.
     */
    suspend fun pickMultipleImages(maxItems: Int? = null): List<PlatformFile>? = FileKit.openFilePicker(
        type = FileKitType.Image,
        mode = FileKitMode.Multiple(maxItems = maxItems),
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    /**
     * Opens the camera picker to capture a photo.
     * On Android/iOS: native camera. On Desktop: falls back to image file picker.
     *
     * @return The captured image file, or null if cancelled or capture failed.
     */
    suspend fun capturePhoto(): PlatformFile? = platformCapturePhoto()

    // ==================== Generic pickers ====================

    /**
     * Opens a generic file picker.
     *
     * @param extensions Allowed file extensions (e.g. "pdf", "docx"). Null allows all.
     * @return The selected file, or null if cancelled.
     */
    suspend fun pickSingleFile(extensions: List<String>? = null): PlatformFile? = FileKit.openFilePicker(
        type = if (extensions != null) FileKitType.File(extensions) else FileKitType.File(),
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    /**
     * Opens a generic file picker for multiple files.
     *
     * @param extensions Allowed file extensions. Null allows all.
     * @param maxItems Maximum number of files (1..50). Null means no limit.
     * @return The selected files, or null if cancelled.
     */
    suspend fun pickMultipleFiles(
        extensions: List<String>? = null,
        maxItems: Int? = null,
    ): List<PlatformFile>? = FileKit.openFilePicker(
        type = if (extensions != null) FileKitType.File(extensions) else FileKitType.File(),
        mode = FileKitMode.Multiple(maxItems = maxItems),
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    /**
     * Opens the directory picker.
     *
     * @return The selected directory, or null if cancelled.
     */
    suspend fun pickDirectory(): PlatformFile? = FileKit.openDirectoryPicker(
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    // ==================== Save / export ====================

    /**
     * Opens the file saver dialog so the user can choose where to save a file.
     * The file is not created automatically; the caller must write data to the
     * returned [PlatformFile].
     *
     * @param suggestedName Default filename without extension.
     * @param extension File extension without dot (e.g. "pdf", "jpg").
     * @return The selected save location, or null if cancelled.
     */
    suspend fun openFileSaver(
        suggestedName: String,
        extension: String,
    ): PlatformFile? = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension,
        directory = null,
        dialogSettings = emptyTitleSettings,
    )

    /**
     * Saves [bytes] to a user-chosen location via the file saver dialog.
     *
     * @param bytes The data to write.
     * @param suggestedName Default filename without extension.
     * @param extension File extension without dot.
     * @return true if the file was saved successfully, false if cancelled or write failed.
     */
    suspend fun saveFileCopy(
        bytes: ByteArray,
        suggestedName: String,
        extension: String,
    ): Boolean {
        val file = openFileSaver(suggestedName, extension) ?: return false
        return runCatching {
            withContext(Dispatchers.IO) {
                file.write(bytes)
            }
        }.isSuccess
    }

    /**
     * Saves the content of [sourceFile] to a user-chosen location via the file saver dialog.
     *
     * @param sourceFile The file to copy from.
     * @param suggestedName Default filename without extension.
     * @param extension File extension without dot.
     * @return true if the file was saved successfully, false if cancelled or write failed.
     */
    suspend fun saveFileCopy(
        sourceFile: PlatformFile,
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

    // ==================== Share ====================

    /**
     * Opens the native share dialog for an imagemark's image file.
     * Resolves the image internally; callers need not access [PlatformFile].
     *
     * @param bookmarkId The imagemark bookmark ID.
     */
    suspend fun shareImageBookmark(bookmarkId: String) {
        val file = ImagemarkFileManager.getImageFile(bookmarkId) ?: return
        platformShareFile(file)
    }

    /**
     * Saves an imagemark's image to a user-chosen location via the file saver dialog.
     *
     * @param bookmarkId The imagemark bookmark ID.
     * @param suggestedName Default filename without extension.
     * @param extension File extension without dot.
     * @return true if the file was saved successfully, false if cancelled or write failed.
     */
    suspend fun exportImageBookmark(
        bookmarkId: String,
        suggestedName: String,
        extension: String,
    ): Boolean {
        val file = ImagemarkFileManager.getImageFile(bookmarkId) ?: return false
        return saveFileCopy(file, suggestedName, extension)
    }

    suspend fun shareDocmark(bookmarkId: String) {
        val type = DatabaseProvider.docBookmarkDao.getByBookmarkId(bookmarkId)?.type ?: DocmarkType.PDF
        val file = DocmarkFileManager.getDocumentFile(bookmarkId, type) ?: return
        platformShareFile(file)
    }

    suspend fun exportDocmark(
        bookmarkId: String,
        suggestedName: String,
        extension: String? = null,
    ): Boolean {
        val type = DatabaseProvider.docBookmarkDao.getByBookmarkId(bookmarkId)?.type ?: DocmarkType.PDF
        val file = DocmarkFileManager.getDocumentFile(bookmarkId, type) ?: return false
        val ext = extension ?: DocmarkFileManager.extensionForType(type)
        return saveFileCopy(file, suggestedName, ext)
    }

    /**
     * Writes `*.md` and optional `assets/` files into a user-selected directory from
     * [NotemarkMarkdownExportBundleDto] JSON produced by the editor WebView.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun saveNotemarkMarkdownExport(
        directory: PlatformFile,
        suggestedBaseName: String,
        bundleJson: String,
    ): Boolean {
        val dto =
            runCatching {
                jsonParser.decodeFromString<NotemarkMarkdownExportBundleDto>(bundleJson)
            }.getOrNull() ?: return false
        val base = sanitizeExportBaseName(suggestedBaseName)
        return runCatching {
            withContext(Dispatchers.IO) {
                val mdFile = directory / "$base.md"
                mdFile.parent()?.createDirectories()
                mdFile.write(dto.markdown.encodeToByteArray())
                for (asset in dto.assets) {
                    val target = directory / asset.relativePath
                    target.parent()?.createDirectories()
                    val bytes = Base64.decode(asset.dataBase64)
                    target.write(bytes)
                }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Writes `*.pdf` into a user-selected directory (same flow as [saveNotemarkMarkdownExport]).
     * Accepts raw base64 or `data:application/pdf;base64,...` from [jsPDF] output.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun saveNotemarkPdfExport(
        directory: PlatformFile,
        suggestedBaseName: String,
        pdfBase64: String,
    ): Boolean {
        val payload = sanitizePdfBase64Payload(pdfBase64) ?: return false
        val bytes =
            runCatching { Base64.decode(payload) }.getOrNull() ?: return false
        if (bytes.isEmpty()) return false
        val base = sanitizeExportBaseName(suggestedBaseName)
        return runCatching {
            withContext(Dispatchers.IO) {
                val pdfFile = directory / "$base.pdf"
                pdfFile.parent()?.createDirectories()
                pdfFile.write(bytes)
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Strips data-URL prefix; rejects JSON (e.g. markdown bundle sent to the PDF path by mistake).
     */
    private fun sanitizePdfBase64Payload(pdfBase64: String): String? {
        var t = pdfBase64.trim()
        if (t.isEmpty()) return null
        if (t.startsWith('{')) return null
        if (t.startsWith("data:", ignoreCase = true)) {
            val idx = t.indexOf("base64,")
            if (idx >= 0) {
                t = t.substring(idx + "base64,".length)
            }
        }
        return t.ifBlank { null }
    }

    private fun sanitizeExportBaseName(label: String): String =
        label.ifBlank { "note" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    /**
     * Opens the native share dialog for a single file.
     * On Android/iOS: system share sheet. On Desktop: opens with default app.
     *
     * @param file The file to share. Must exist and be accessible.
     */
    internal suspend fun shareFile(file: PlatformFile) {
        platformShareFile(file)
    }

    /**
     * Opens the native share dialog for multiple files.
     * On Android/iOS: system share sheet. On Desktop: opens first file with default app.
     *
     * @param files The files to share. Must exist and be accessible.
     */
    suspend fun shareFiles(files: List<PlatformFile>) {
        platformShareFiles(files)
    }

    // ==================== Open with default app ====================

    /**
     * Opens the file with the system's default application for its type.
     *
     * @param file The file to open. Must exist and be accessible.
     */
    fun openFile(file: PlatformFile) {
        FileKit.openFileWithDefaultApplication(file)
    }

}
