package dev.subfly.yaba.core.state.detail.docmark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.utils.DocmarkType
import dev.subfly.yaba.core.model.utils.ReaderPreferences
import dev.subfly.yaba.core.webview.Toc

@Immutable
data class DocmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val summary: String? = null,
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
    val docmarkType: DocmarkType = DocmarkType.PDF,
    val documentAbsolutePath: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val selectedReadableVersionId: String? = null,
    val annotations: List<AnnotationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val webContentLoadFailed: Boolean = false,
    val reminderDateEpochMillis: Long? = null,
    val scrollToAnnotationId: String? = null,
    val toc: Toc? = null,
    val pendingTocNavigate: Pair<String, String?>? = null,
)
