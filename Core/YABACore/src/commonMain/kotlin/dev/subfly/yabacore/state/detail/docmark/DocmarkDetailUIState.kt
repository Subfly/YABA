package dev.subfly.yabacore.state.detail.docmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel

@Immutable
data class DocmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val summary: String? = null,
    val pdfAbsolutePath: String? = null,
    val selectedReadableVersionId: String? = null,
    val annotations: List<AnnotationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToAnnotationId: String? = null,
)
