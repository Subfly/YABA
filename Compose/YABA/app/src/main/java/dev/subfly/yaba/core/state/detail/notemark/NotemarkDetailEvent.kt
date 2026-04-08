package dev.subfly.yaba.core.state.detail.notemark

import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.WebShellLoadResult

sealed interface NotemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : NotemarkDetailEvent

    /**
     * Persist a snapshot of the editor document (JSON). The UI obtains this from
     * [dev.subfly.yaba.core.webview.WebViewEditorBridge.getDocumentJson] on lifecycle pause /
     * disposal, and [dev.subfly.yaba.core.webview.WebViewEditorBridge.getUsedInlineAssetSrcs]
     * so Core can delete on-disk `assets/` files no longer referenced by the document.
     */
    data class OnSave(
        val documentJson: String,
        val usedInlineAssetSrcs: List<String> = emptyList(),
    ) : NotemarkDetailEvent

    data object OnDeleteBookmark : NotemarkDetailEvent

    data object OnRequestNotificationPermission : NotemarkDetailEvent

    data class OnScheduleReminder(
        val title: String,
        val message: String,
        val triggerAtEpochMillis: Long,
    ) : NotemarkDetailEvent

    data object OnCancelReminder : NotemarkDetailEvent

    /**
     * Opens the gallery picker; Core saves bytes and sets
     * [NotemarkDetailUIState.inlineImageDocumentSrc].
     */
    data object OnPickImageFromGallery : NotemarkDetailEvent

    /**
     * Opens the camera; Core saves bytes and sets [NotemarkDetailUIState.inlineImageDocumentSrc].
     */
    data object OnCaptureImageFromCamera : NotemarkDetailEvent

    /**
     * Clears [NotemarkDetailUIState.inlineImageDocumentSrc] after the bridge has inserted the
     * image.
     */
    data object OnConsumedInlineImageInsert : NotemarkDetailEvent

    /** One-shot: WebView editor finished initial document application (success or error). */
    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : NotemarkDetailEvent

    data class OnTocChanged(val toc: Toc?) : NotemarkDetailEvent
    data class OnNavigateToTocItem(val id: String, val extrasJson: String?) : NotemarkDetailEvent
    data object OnClearTocNavigation : NotemarkDetailEvent

    /**
     * UI ran [dev.subfly.yaba.core.webview.WebViewEditorBridge.exportNoteMarkdownBundleJson];
     * Core opens the directory picker and writes `.md` + assets via [dev.subfly.yaba.core.filesystem.access.YabaFileAccessor].
     */
    data class OnExportMarkdownReady(val markdown: String) : NotemarkDetailEvent

    /**
     * UI ran [dev.subfly.yaba.core.webview.WebViewEditorBridge.exportNotePdfBase64];
     * Core writes `.pdf` via [dev.subfly.yaba.core.filesystem.access.YabaFileAccessor.saveFileCopy].
     */
    data class OnExportPdfReady(val pdfBase64: String) : NotemarkDetailEvent
}
