package dev.subfly.yaba.core.state.creation.linkmark

import androidx.compose.runtime.Stable
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.LinkmarkUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.unfurl.ReadableUnfurl

/** UI state for linkmark (link bookmark) creation/editing. */
@Stable
data class LinkmarkCreationUIState(
    // URL-related state
    val url: String = "",
    val cleanedUrl: String = "",
    val host: String = "",
    val lastFetchedUrl: String = "",

    // User-editable bookmark fields (not autofilled from scrape)
    val label: String = "",
    val description: String = "",

    // Read-only extraction metadata (from WebView converter; same naming as docmarks)
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,

    // Structured readable content extracted via converter
    val readable: ReadableUnfurl? = null,

    // Binary data (in-memory only, not persisted)
    val imageData: ByteArray? = null,
    val iconData: ByteArray? = null,

    // Collection associations
    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),

    // Converter flow: raw HTML to be converted by WebView (when both set, conversion runs)
    val converterHtml: String? = null,
    val converterBaseUrl: String? = null,
    val converterError: String? = null,

    // Loading and error states
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: LinkmarkCreationError? = null,

    // Preview appearance (initialized from preferences, then user-controlled via cycling)
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,

    // Edit mode
    val editingLinkmark: LinkmarkUiModel? = null,

    /** When saving, persisted via [dev.subfly.yaba.core.managers.AllBookmarksManager]. */
    val isPrivate: Boolean = false,

    val isPinned: Boolean = false,

    // Flag to indicate if uncategorized folder needs to be created
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingLinkmark != null

    val hasError: Boolean
        get() = error != null

    val canSave: Boolean
        get() =
            label.trim().isNotEmpty() &&
                cleanedUrl.isNotBlank() &&
                selectedFolder != null &&
                !isLoading

    /** True when the converter provided a title or description to copy into user fields. */
    val hasApplyableMetadata: Boolean
        get() = metadataTitle.isNullOrBlank().not() || metadataDescription.isNullOrBlank().not()

    // Override equals/hashCode to handle ByteArray comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LinkmarkCreationUIState

        if (url != other.url) return false
        if (cleanedUrl != other.cleanedUrl) return false
        if (host != other.host) return false
        if (lastFetchedUrl != other.lastFetchedUrl) return false
        if (label != other.label) return false
        if (description != other.description) return false
        if (metadataTitle != other.metadataTitle) return false
        if (metadataDescription != other.metadataDescription) return false
        if (metadataAuthor != other.metadataAuthor) return false
        if (metadataDate != other.metadataDate) return false
        if (videoUrl != other.videoUrl) return false
        if (audioUrl != other.audioUrl) return false
        if (readable != other.readable) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false
        if (selectedFolder != other.selectedFolder) return false
        if (selectedTags != other.selectedTags) return false
        if (converterHtml != other.converterHtml) return false
        if (converterBaseUrl != other.converterBaseUrl) return false
        if (converterError != other.converterError) return false
        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (bookmarkAppearance != other.bookmarkAppearance) return false
        if (cardImageSizing != other.cardImageSizing) return false
        if (editingLinkmark != other.editingLinkmark) return false
        if (isPrivate != other.isPrivate) return false
        if (isPinned != other.isPinned) return false
        if (uncategorizedFolderCreationRequired != other.uncategorizedFolderCreationRequired)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + cleanedUrl.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + lastFetchedUrl.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (metadataTitle?.hashCode() ?: 0)
        result = 31 * result + (metadataDescription?.hashCode() ?: 0)
        result = 31 * result + (metadataAuthor?.hashCode() ?: 0)
        result = 31 * result + (metadataDate?.hashCode() ?: 0)
        result = 31 * result + (videoUrl?.hashCode() ?: 0)
        result = 31 * result + (audioUrl?.hashCode() ?: 0)
        result = 31 * result + (readable?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        result = 31 * result + (selectedFolder?.hashCode() ?: 0)
        result = 31 * result + selectedTags.hashCode()
        result = 31 * result + (converterHtml?.hashCode() ?: 0)
        result = 31 * result + (converterBaseUrl?.hashCode() ?: 0)
        result = 31 * result + (converterError?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + bookmarkAppearance.hashCode()
        result = 31 * result + cardImageSizing.hashCode()
        result = 31 * result + (editingLinkmark?.hashCode() ?: 0)
        result = 31 * result + isPrivate.hashCode()
        result = 31 * result + isPinned.hashCode()
        result = 31 * result + uncategorizedFolderCreationRequired.hashCode()
        return result
    }
}
