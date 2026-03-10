package dev.subfly.yabacore.state.creation.imagemark

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.ImagemarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing

/**
 * UI state for imagemark creation/editing.
 */
@Stable
data class ImagemarkCreationUIState(
    val imageBytes: ByteArray? = null,
    val imageExtension: String = "jpeg",

    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,

    val label: String = "",
    val description: String = "",
    val summary: String = "",

    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: ImagemarkCreationError? = null,

    val editingImagemark: ImagemarkUiModel? = null,
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingImagemark != null

    val hasError: Boolean
        get() = error != null

    val canSave: Boolean
        get() = imageBytes != null &&
            selectedFolder != null &&
            label.isNotBlank() &&
            !isLoading

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ImagemarkCreationUIState
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false
        if (imageExtension != other.imageExtension) return false
        if (bookmarkAppearance != other.bookmarkAppearance) return false
        if (cardImageSizing != other.cardImageSizing) return false
        if (label != other.label) return false
        if (description != other.description) return false
        if (summary != other.summary) return false
        if (selectedFolder != other.selectedFolder) return false
        if (selectedTags != other.selectedTags) return false
        if (isLoading != other.isLoading) return false
        if (isSaving != other.isSaving) return false
        if (error != other.error) return false
        if (editingImagemark != other.editingImagemark) return false
        if (uncategorizedFolderCreationRequired != other.uncategorizedFolderCreationRequired) return false
        return true
    }

    override fun hashCode(): Int {
        var result = imageBytes?.contentHashCode() ?: 0
        result = 31 * result + imageExtension.hashCode()
        result = 31 * result + bookmarkAppearance.hashCode()
        result = 31 * result + cardImageSizing.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + (selectedFolder?.hashCode() ?: 0)
        result = 31 * result + selectedTags.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (editingImagemark?.hashCode() ?: 0)
        result = 31 * result + uncategorizedFolderCreationRequired.hashCode()
        return result
    }
}
