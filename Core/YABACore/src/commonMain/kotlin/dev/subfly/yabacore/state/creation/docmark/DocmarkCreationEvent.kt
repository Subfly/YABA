package dev.subfly.yabacore.state.creation.docmark

import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.webview.WebShellLoadResult

sealed class DocmarkCreationEvent {
    data class OnInit(
        val docmarkIdString: String? = null,
        val initialFolderId: String? = null,
        val initialTagIds: List<String>? = null,
    ) : DocmarkCreationEvent()

    data object OnPickDocument : DocmarkCreationEvent()

    data object OnClearDocument : DocmarkCreationEvent()

    data class OnDocumentFromShare(
        val bytes: ByteArray,
        val sourceFileName: String?,
        val docmarkType: DocmarkType,
    ) : DocmarkCreationEvent() {
        override fun equals(other: Any?): Boolean =
            other is OnDocumentFromShare &&
                bytes.contentEquals(other.bytes) &&
                sourceFileName == other.sourceFileName &&
                docmarkType == other.docmarkType

        override fun hashCode(): Int =
            bytes.contentHashCode() * 31 + (sourceFileName?.hashCode() ?: 0) + docmarkType.hashCode()
    }

    data object OnCyclePreviewAppearance : DocmarkCreationEvent()

    data class OnSetGeneratedPreview(
        val imageBytes: ByteArray?,
        val extension: String = "png",
    ) : DocmarkCreationEvent() {
        override fun equals(other: Any?): Boolean =
            other is OnSetGeneratedPreview &&
                extension == other.extension &&
                if (imageBytes == null && other.imageBytes == null) true
                else imageBytes != null && other.imageBytes != null && imageBytes.contentEquals(
                    other.imageBytes,
                )

        override fun hashCode(): Int =
            (imageBytes?.contentHashCode() ?: 0) * 31 + extension.hashCode()
    }

    data class OnChangeLabel(val newLabel: String) : DocmarkCreationEvent()

    data class OnChangeDescription(val newDescription: String) : DocmarkCreationEvent()

    data class OnChangeSummary(val newSummary: String) : DocmarkCreationEvent()

    data class OnSelectFolder(val folder: FolderUiModel) : DocmarkCreationEvent()

    data class OnSelectTags(val tags: List<TagUiModel>) : DocmarkCreationEvent()

    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: () -> Unit,
    ) : DocmarkCreationEvent()

    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : DocmarkCreationEvent()
}
