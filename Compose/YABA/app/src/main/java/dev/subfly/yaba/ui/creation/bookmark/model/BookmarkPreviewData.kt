package dev.subfly.yaba.ui.creation.bookmark.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel

@Stable
@Immutable
/** Data for the shared bookmark preview card. */
data class BookmarkPreviewData(
    val imageData: ByteArray?,
    val domainImageData: ByteArray? = null,
    val label: String,
    val description: String,
    val selectedFolder: FolderUiModel?,
    val selectedTags: List<TagUiModel>,
    val isLoading: Boolean,
    val emptyImageIconName: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookmarkPreviewData

        if (isLoading != other.isLoading) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (!domainImageData.contentEquals(other.domainImageData)) return false
        if (label != other.label) return false
        if (description != other.description) return false
        if (selectedFolder != other.selectedFolder) return false
        if (selectedTags != other.selectedTags) return false
        if (emptyImageIconName != other.emptyImageIconName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (domainImageData?.contentHashCode() ?: 0)
        result = 31 * result + label.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (selectedFolder?.hashCode() ?: 0)
        result = 31 * result + selectedTags.hashCode()
        result = 31 * result + emptyImageIconName.hashCode()
        return result
    }
}
