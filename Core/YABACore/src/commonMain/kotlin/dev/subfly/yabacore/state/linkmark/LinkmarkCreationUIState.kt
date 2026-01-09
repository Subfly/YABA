package dev.subfly.yabacore.state.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.LinkType

/**
 * UI state for linkmark (link bookmark) creation/editing.
 *
 * Mirrors the Swift BookmarkCreationState with adaptations for KMP.
 */
@Immutable
data class LinkmarkCreationUIState(
    // URL-related state
    val url: String = "",
    val cleanedUrl: String = "",
    val host: String = "",
    val lastFetchedUrl: String = "",

    // Content metadata
    val label: String = "",
    val description: String = "",
    val iconUrl: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val readableHtml: String? = null,

    // Binary data (in-memory only, not persisted)
    val imageData: ByteArray? = null,
    val iconData: ByteArray? = null,

    // Link type classification
    val selectedLinkType: LinkType = LinkType.NONE,

    // Collection associations
    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),

    // Loading and error states
    val isLoading: Boolean = false,
    val error: LinkmarkCreationError? = null,

    // Preview appearance (initialized from preferences, then user-controlled via cycling)
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,

    // Edit mode
    val editingLinkmark: LinkmarkUiModel? = null,

    // Flag to indicate if uncategorized folder needs to be created
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingLinkmark != null

    val hasError: Boolean
        get() = error != null

    val canSave: Boolean
        get() = cleanedUrl.isNotBlank() && selectedFolder != null && !isLoading

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
        if (iconUrl != other.iconUrl) return false
        if (imageUrl != other.imageUrl) return false
        if (videoUrl != other.videoUrl) return false
        if (readableHtml != other.readableHtml) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false
        if (selectedLinkType != other.selectedLinkType) return false
        if (selectedFolder != other.selectedFolder) return false
        if (selectedTags != other.selectedTags) return false
        if (isLoading != other.isLoading) return false
        if (error != other.error) return false
        if (bookmarkAppearance != other.bookmarkAppearance) return false
        if (cardImageSizing != other.cardImageSizing) return false
        if (editingLinkmark != other.editingLinkmark) return false
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
        result = 31 * result + (iconUrl?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + (videoUrl?.hashCode() ?: 0)
        result = 31 * result + (readableHtml?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        result = 31 * result + selectedLinkType.hashCode()
        result = 31 * result + (selectedFolder?.hashCode() ?: 0)
        result = 31 * result + selectedTags.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + bookmarkAppearance.hashCode()
        result = 31 * result + cardImageSizing.hashCode()
        result = 31 * result + (editingLinkmark?.hashCode() ?: 0)
        result = 31 * result + uncategorizedFolderCreationRequired.hashCode()
        return result
    }
}
