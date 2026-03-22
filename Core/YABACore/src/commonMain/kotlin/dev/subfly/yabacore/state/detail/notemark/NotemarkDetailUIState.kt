package dev.subfly.yabacore.state.detail.notemark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Immutable
data class NotemarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    /** Stable readable layer id for highlight anchors. */
    val readableVersionId: String? = null,
    /** Base URL for resolving relative assets in the editor (`file://.../bookmarks/<id>/`). */
    val assetsBaseUrl: String? = null,
    /**
     * Document JSON loaded once from disk for bootstrapping the WebView editor.
     * Live edits stay in the WebView; persistence is triggered by [NotemarkDetailEvent.OnSave].
     */
    val initialDocumentJson: String? = null,
    val highlights: List<HighlightUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToHighlightId: String? = null,
)
