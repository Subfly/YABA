package dev.subfly.yabacore.state.detail.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences

@Immutable
data class LinkmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val linkDetails: LinkmarkLinkDetailsUiModel? = null,
    val readableVersions: List<ReadableVersionUiModel> = emptyList(),
    /** Currently selected version ID; null means use newest (first). */
    val selectedReadableVersionId: String? = null,
    /** Sanitized reader HTML for WebView viewer. */
    val readableHtml: String? = null,
    /** Base URL for resolving ../assets/ in HTML (file://... with trailing slash). */
    val assetsBaseUrl: String? = null,
    val readerPreferences: ReaderPreferences = ReaderPreferences(),
    val highlights: List<HighlightUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdatingReadable: Boolean = false,
    /** Converter flow: raw HTML to be converted by WebView. */
    val converterHtml: String? = null,
    val converterBaseUrl: String? = null,
    val converterError: String? = null,
    /** Scheduled reminder fire date as epoch millis, null when no reminder is pending. */
    val reminderDateEpochMillis: Long? = null,
    val hasNotificationPermission: Boolean = false,
    /** When set, the reader should scroll to this highlight and then clear. */
    val scrollToHighlightId: String? = null,
)

@Immutable
data class LinkmarkLinkDetailsUiModel(
    val url: String,
    val domain: String,
    val videoUrl: String?,
)
