package dev.subfly.yabacore.state.creation.linkmark

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.unfurl.ReadableUnfurl
import kotlin.collections.iterator

/**
 * UI state for linkmark (link bookmark) creation/editing.
 *
 * Mirrors the Swift BookmarkCreationState with adaptations for KMP.
 */
@Stable
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

        // Structured readable content extracted from HTML
        val readable: ReadableUnfurl? = null,

        // Binary data (in-memory only, not persisted)
        val imageData: ByteArray? = null,
        val iconData: ByteArray? = null,

        // Selectable images from unfurling (URL -> ByteArray)
        val selectableImages: Map<String, ByteArray> = emptyMap(),

        // Content update indicator (edit mode): unfurl can detect newer assets/content.
        // UI can show an "Has updates" button when this is true.
        // Pending updates (applied only when user clicks the "apply updates" action).
        val hasContentUpdates: Boolean = false,
        val updateImageData: ByteArray? = null,
        val updateIconData: ByteArray? = null,
        val shouldUpdateVideoUrl: Boolean = false,
        val updateVideoUrl: String? = null,
        val shouldUpdateReadable: Boolean = false,
        val updateReadable: ReadableUnfurl? = null,

        // Link type classification
        val selectedLinkType: LinkType = LinkType.NONE,

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

        // Flag to indicate if uncategorized folder needs to be created
        val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingLinkmark != null

    val hasError: Boolean
        get() = error != null

    val canSave: Boolean
        get() = cleanedUrl.isNotBlank() && selectedFolder != null && !isLoading

    /** List of selectable image URLs for navigation to image selection screen. */
    val selectableImageUrls: List<String>
        get() = selectableImages.keys.toList()

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
        if (readable != other.readable) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false
        if (!selectableImagesEqual(selectableImages, other.selectableImages)) return false
        if (hasContentUpdates != other.hasContentUpdates) return false
        if (updateImageData != null) {
            if (other.updateImageData == null) return false
            if (!updateImageData.contentEquals(other.updateImageData)) return false
        } else if (other.updateImageData != null) return false
        if (updateIconData != null) {
            if (other.updateIconData == null) return false
            if (!updateIconData.contentEquals(other.updateIconData)) return false
        } else if (other.updateIconData != null) return false
        if (shouldUpdateVideoUrl != other.shouldUpdateVideoUrl) return false
        if (updateVideoUrl != other.updateVideoUrl) return false
        if (shouldUpdateReadable != other.shouldUpdateReadable) return false
        if (updateReadable != other.updateReadable) return false
        if (selectedLinkType != other.selectedLinkType) return false
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
        result = 31 * result + (readable?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        result = 31 * result + selectableImagesHashCode(selectableImages)
        result = 31 * result + hasContentUpdates.hashCode()
        result = 31 * result + (updateImageData?.contentHashCode() ?: 0)
        result = 31 * result + (updateIconData?.contentHashCode() ?: 0)
        result = 31 * result + shouldUpdateVideoUrl.hashCode()
        result = 31 * result + (updateVideoUrl?.hashCode() ?: 0)
        result = 31 * result + shouldUpdateReadable.hashCode()
        result = 31 * result + (updateReadable?.hashCode() ?: 0)
        result = 31 * result + selectedLinkType.hashCode()
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
        result = 31 * result + uncategorizedFolderCreationRequired.hashCode()
        return result
    }

    private fun selectableImagesEqual(
            a: Map<String, ByteArray>,
            b: Map<String, ByteArray>,
    ): Boolean {
        if (a.size != b.size) return false
        for ((key, value) in a) {
            val otherValue = b[key] ?: return false
            if (!value.contentEquals(otherValue)) return false
        }
        return true
    }

    private fun selectableImagesHashCode(map: Map<String, ByteArray>): Int {
        var result = 0
        for ((key, value) in map) {
            result = 31 * result + key.hashCode()
            result = 31 * result + value.contentHashCode()
        }
        return result
    }
}
