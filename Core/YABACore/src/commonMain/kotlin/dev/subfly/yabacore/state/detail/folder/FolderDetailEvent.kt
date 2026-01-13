package dev.subfly.yabacore.state.detail.folder

import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface FolderDetailEvent {
    data class OnInit(val folderId: String) : FolderDetailEvent
    data class OnChangeQuery(val query: String) : FolderDetailEvent
    data object OnToggleSelectionMode : FolderDetailEvent
    data class OnToggleBookmarkSelection(val bookmarkId: Uuid) : FolderDetailEvent
    data class OnMoveSelectedToFolder(val targetFolder: FolderUiModel) : FolderDetailEvent
    data object OnDeleteSelected : FolderDetailEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : FolderDetailEvent
    data class OnChangeAppearance(val appearance: BookmarkAppearance) : FolderDetailEvent
}
