package dev.subfly.yabacore.state.creation.docmark

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.ui.DocmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.DocmarkType

@Stable
data class DocmarkCreationUIState(
    val documentBytes: ByteArray? = null,
    val docmarkType: DocmarkType? = null,
    val sourceFileName: String? = null,
    val label: String = "",
    val description: String = "",
    val summary: String = "",
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,

    val previewImageBytes: ByteArray? = null,
    val previewImageExtension: String = "png",
    val selectedFolder: FolderUiModel? = null,
    val selectedTags: List<TagUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: DocmarkCreationError? = null,
    val editingDocmark: DocmarkUiModel? = null,
    val uncategorizedFolderCreationRequired: Boolean = false,
) {
    val isInEditMode: Boolean
        get() = editingDocmark != null

    val canSave: Boolean
        get() = selectedFolder != null &&
            (isInEditMode || documentBytes != null) &&
            !isLoading

    val hasApplyableMetadata: Boolean
        get() = metadataTitle.isNullOrBlank().not() || metadataDescription.isNullOrBlank().not()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocmarkCreationUIState) return false

        if (documentBytes != null) {
            if (other.documentBytes == null || !documentBytes.contentEquals(other.documentBytes)) return false
        } else if (other.documentBytes != null) return false

        if (docmarkType != other.docmarkType) return false
        if (sourceFileName != other.sourceFileName) return false
        if (bookmarkAppearance != other.bookmarkAppearance) return false
        if (cardImageSizing != other.cardImageSizing) return false
        if (label != other.label) return false
        if (description != other.description) return false
        if (summary != other.summary) return false
        if (metadataTitle != other.metadataTitle) return false
        if (metadataDescription != other.metadataDescription) return false
        if (metadataAuthor != other.metadataAuthor) return false
        if (metadataDate != other.metadataDate) return false
        if (previewImageBytes != null) {
            if (other.previewImageBytes == null || !previewImageBytes.contentEquals(other.previewImageBytes)) return false
        } else if (other.previewImageBytes != null) return false
        if (previewImageExtension != other.previewImageExtension) return false
        if (selectedFolder != other.selectedFolder) return false
        if (selectedTags != other.selectedTags) return false
        if (isLoading != other.isLoading) return false
        if (isSaving != other.isSaving) return false
        if (error != other.error) return false
        if (editingDocmark != other.editingDocmark) return false
        if (uncategorizedFolderCreationRequired != other.uncategorizedFolderCreationRequired) return false
        return true
    }

    override fun hashCode(): Int {
        var result = documentBytes?.contentHashCode() ?: 0
        result = 31 * result + (docmarkType?.hashCode() ?: 0)
        result = 31 * result + (sourceFileName?.hashCode() ?: 0)
        result = 31 * result + bookmarkAppearance.hashCode()
        result = 31 * result + cardImageSizing.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + (metadataTitle?.hashCode() ?: 0)
        result = 31 * result + (metadataDescription?.hashCode() ?: 0)
        result = 31 * result + (metadataAuthor?.hashCode() ?: 0)
        result = 31 * result + (metadataDate?.hashCode() ?: 0)
        result = 31 * result + (previewImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + previewImageExtension.hashCode()
        result = 31 * result + (selectedFolder?.hashCode() ?: 0)
        result = 31 * result + selectedTags.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + isSaving.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (editingDocmark?.hashCode() ?: 0)
        result = 31 * result + uncategorizedFolderCreationRequired.hashCode()
        return result
    }
}
