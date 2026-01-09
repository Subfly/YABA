package dev.subfly.yabacore.state.home

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.CollectionAppearance
import dev.subfly.yabacore.model.utils.DropZone
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

sealed class HomeEvent {
    // Initialization
    data object OnInit : HomeEvent()

    // Preference changes
    data class OnChangeCollectionAppearance(val appearance: CollectionAppearance) : HomeEvent()
    data class OnChangeBookmarkAppearance(val appearance: BookmarkAppearance) : HomeEvent()
    data class OnChangeCardImageSizing(val sizing: CardImageSizing) : HomeEvent()
    data class OnChangeCollectionSorting(val sortType: SortType) : HomeEvent()
    data class OnChangeSortOrder(val sortOrder: SortOrderType) : HomeEvent()

    // Folder operations
    data class OnDeleteFolder(val folder: FolderUiModel) : HomeEvent()
    data class OnMoveFolder(
        val folder: FolderUiModel,
        val targetParent: FolderUiModel?,
    ) : HomeEvent()

    data class OnReorderFolder(
        val dragged: FolderUiModel,
        val target: FolderUiModel,
        val zone: DropZone,
    ) : HomeEvent()

    // Tag operations
    data class OnDeleteTag(val tag: TagUiModel) : HomeEvent()
    data class OnReorderTag(
        val dragged: TagUiModel,
        val target: TagUiModel,
        val zone: DropZone,
    ) : HomeEvent()

    // Bookmark operations
    data class OnDeleteBookmark(val bookmark: BookmarkUiModel) : HomeEvent()
    data class OnMoveBookmarkToFolder(
        val bookmark: BookmarkUiModel,
        val targetFolder: FolderUiModel,
    ) : HomeEvent()

    data class OnMoveBookmarkToTag(
        val bookmark: BookmarkUiModel,
        val targetTag: TagUiModel,
    ) : HomeEvent()
}
