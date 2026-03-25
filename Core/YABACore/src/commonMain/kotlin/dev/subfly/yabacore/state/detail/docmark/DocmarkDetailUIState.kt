package dev.subfly.yabacore.state.detail.docmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.model.utils.ReaderPreferences

@Immutable
data class DocmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val summary: String? = null,
    val docmarkType: DocmarkType = DocmarkType.PDF,
    /** Absolute path to `document.pdf` or `document.epub`. */
    val documentAbsolutePath: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val selectedReadableVersionId: String? = null,
    val annotations: List<AnnotationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToAnnotationId: String? = null,
)
