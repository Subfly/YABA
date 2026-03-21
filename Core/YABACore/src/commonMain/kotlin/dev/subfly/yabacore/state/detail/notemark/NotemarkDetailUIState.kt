package dev.subfly.yabacore.state.detail.notemark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.NoteSaveMode

@Immutable
data class NotemarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    /** Stable readable layer id for highlight anchors. */
    val readableVersionId: String? = null,
    /** Base URL for resolving relative assets in the editor (`file://.../bookmarks/<id>/`). */
    val assetsBaseUrl: String? = null,
    /** Document JSON last loaded from disk / last successfully persisted. */
    val lastSavedDocumentJson: String = "",
    /** Current editor buffer (mirrors WebView). */
    val editorDocumentJson: String = "",
    val highlights: List<HighlightUiModel> = emptyList(),
    val saveMode: NoteSaveMode = NoteSaveMode.AUTOSAVE_3S_INACTIVITY,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToHighlightId: String? = null,
)
