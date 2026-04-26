package dev.subfly.yaba.core.state.detail.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.utils.ReaderPreferences
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.computeDetailWebShellPhase
import dev.subfly.yaba.core.webview.Toc

@Immutable
data class LinkmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val linkDetails: LinkmarkLinkDetailsUiModel? = null,
    /** Rich-text document JSON for the WebView reader (same schema as the note editor body). */
    val readableDocumentJson: String? = null,
    /** Base URL for resolving ../assets/ in document JSON (file://... with trailing slash). */
    val assetsBaseUrl: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val annotations: List<AnnotationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    /** True when the readable WebView reported a failed initial load (one-shot). */
    val readerWebContentLoadFailed: Boolean = false,
    /** Scheduled reminder fire date as epoch millis, null when no reminder is pending. */
    val reminderDateEpochMillis: Long? = null,
    val hasNotificationPermission: Boolean = false,
    /** When set, the reader should scroll to this annotation and then clear. */
    val scrollToAnnotationId: String? = null,
    val toc: Toc? = null,
    val pendingTocNavigate: Pair<String, String?>? = null,
)

fun LinkmarkDetailUIState.detailWebShellPhase(): DetailWebShellPhase {
    val hasPayload = readableDocumentJson.isNullOrBlank().not()
    return computeDetailWebShellPhase(
        isLoading = isLoading,
        hasWebPayload = hasPayload,
        webContentLoadFailed = readerWebContentLoadFailed,
    )
}

@Immutable
data class LinkmarkLinkDetailsUiModel(
    val url: String,
    val domain: String,
    val videoUrl: String?,
    val audioUrl: String? = null,
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
)
