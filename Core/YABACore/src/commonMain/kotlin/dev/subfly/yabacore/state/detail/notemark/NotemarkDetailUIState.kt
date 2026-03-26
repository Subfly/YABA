package dev.subfly.yabacore.state.detail.notemark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel

@Immutable
data class NotemarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    /** Stable readable layer id for notemark readable mirror (if any). */
    val readableVersionId: String? = null,
    /** Base URL for resolving relative assets in the editor (`file://.../bookmarks/<id>/`). */
    val assetsBaseUrl: String? = null,
    /**
     * Document JSON loaded once from disk for bootstrapping the WebView editor. Live edits stay
     * in the WebView; persistence is triggered by [NotemarkDetailEvent.OnSave].
     */
    val initialDocumentJson: String? = null,
    val isLoading: Boolean = false,
    /** Set when [OnWebInitialContentLoad] reports [dev.subfly.yabacore.webview.WebShellLoadResult.Error]. */
    val webContentLoadFailed: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    /**
     * One-shot: canonical document `src` (`../assets/<id>.<ext>`) after Core saved a
     * gallery/camera image. UI dispatches
     * [dev.subfly.yabacore.webview.YabaEditorCommands.insertImagePayload] then
     * [OnConsumedInlineImageInsert].
     */
    val inlineImageDocumentSrc: String? = null,
)
