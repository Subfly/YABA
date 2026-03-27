package dev.subfly.yabacore.state.detail.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences

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
)

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
