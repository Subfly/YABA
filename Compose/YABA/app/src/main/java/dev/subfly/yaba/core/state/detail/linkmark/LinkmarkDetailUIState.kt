package dev.subfly.yaba.core.state.detail.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.AnnotationUiModel
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.ui.ReadableVersionUiModel
import dev.subfly.yaba.core.model.utils.ReaderPreferences
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.computeDetailWebShellPhase
import dev.subfly.yaba.core.webview.Toc

@Immutable
data class LinkmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val linkDetails: LinkmarkLinkDetailsUiModel? = null,
    val readableVersions: List<ReadableVersionUiModel> = emptyList(),
    /** Currently selected version ID; null means use newest (first). */
    val selectedReadableVersionId: String? = null,
    /** Rich-text document JSON for the WebView reader (same schema as the note editor body). */
    val readableDocumentJson: String? = null,
    /** Base URL for resolving ../assets/ in document JSON (file://... with trailing slash). */
    val assetsBaseUrl: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val annotations: List<AnnotationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    /** True when the readable WebView reported a failed initial load (one-shot). */
    val readerWebContentLoadFailed: Boolean = false,
    val isUpdatingReadable: Boolean = false,
    /** Converter flow: raw HTML to be converted by WebView. */
    val converterHtml: String? = null,
    val converterBaseUrl: String? = null,
    val converterError: String? = null,
    /** Scheduled reminder fire date as epoch millis, null when no reminder is pending. */
    val reminderDateEpochMillis: Long? = null,
    val hasNotificationPermission: Boolean = false,
    /** When set, the reader should scroll to this annotation and then clear. */
    val scrollToAnnotationId: String? = null,
    /** In-memory table of contents from the readable WebView; null until first payload or when cleared. */
    val toc: Toc? = null,
    /** One-shot: navigate reader to this ToC entry ([first] = id, [second] = [TocItem.extrasJson]). */
    val pendingTocNavigate: Pair<String, String?>? = null,
)

fun LinkmarkDetailUIState.detailWebShellPhase(): DetailWebShellPhase {
    val blockingLoad = isLoading || isUpdatingReadable
    val hasPayload = readableDocumentJson.isNullOrBlank().not()
    return computeDetailWebShellPhase(
        isLoading = blockingLoad,
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
