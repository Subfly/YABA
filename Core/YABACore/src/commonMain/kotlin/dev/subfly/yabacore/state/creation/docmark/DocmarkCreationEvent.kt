package dev.subfly.yabacore.state.creation.docmark

import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel

sealed class DocmarkCreationEvent {
    data class OnInit(
        val docmarkIdString: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : DocmarkCreationEvent()

    data object OnPickPdf : DocmarkCreationEvent()

    data object OnClearPdf : DocmarkCreationEvent()

    data class OnSetGeneratedPreview(
        val imageBytes: ByteArray?,
        val extension: String = "png",
    ) : DocmarkCreationEvent() {
        override fun equals(other: Any?): Boolean =
            other is OnSetGeneratedPreview &&
                extension == other.extension &&
                if (imageBytes == null && other.imageBytes == null) true
                else imageBytes != null && other.imageBytes != null && imageBytes.contentEquals(other.imageBytes)

        override fun hashCode(): Int = (imageBytes?.contentHashCode() ?: 0) * 31 + extension.hashCode()
    }

    data class OnSetInternalReadableMarkdown(val markdown: String?) : DocmarkCreationEvent()

    data class OnChangeLabel(val newLabel: String) : DocmarkCreationEvent()

    data class OnChangeDescription(val newDescription: String) : DocmarkCreationEvent()

    data class OnChangeSummary(val newSummary: String) : DocmarkCreationEvent()

    data class OnSelectFolder(val folder: FolderUiModel) : DocmarkCreationEvent()

    data class OnSelectTags(val tags: List<TagUiModel>) : DocmarkCreationEvent()

    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: () -> Unit,
    ) : DocmarkCreationEvent()
}
