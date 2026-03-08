package dev.subfly.yabacore.state.detail.docmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel

@Immutable
data class DocmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val summary: String? = null,
    val pdfAbsolutePath: String? = null,
    val selectedReadableVersionId: String? = null,
    val highlights: List<HighlightUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToHighlightId: String? = null,
)
