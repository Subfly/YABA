package dev.subfly.yaba.core.state.home

import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

sealed class HomeEvent {
    // Initialization
    data object OnInit : HomeEvent()

    // Preference changes
    data class OnChangeBookmarkAppearance(val appearance: BookmarkAppearance) : HomeEvent()
    data class OnChangeCardImageSizing(val sizing: CardImageSizing) : HomeEvent()
    data class OnChangeCollectionSorting(val sortType: SortType) : HomeEvent()
    data class OnChangeSortOrder(val sortOrder: SortOrderType) : HomeEvent()

    // Folder operations
    data class OnToggleFolderExpanded(val folderId: String) : HomeEvent()
    data class OnDeleteFolder(val folder: FolderUiModel) : HomeEvent()
    data class OnMoveFolder(
        val folder: FolderUiModel,
        val targetParent: FolderUiModel?,
    ) : HomeEvent()

    // Tag operations
    data class OnDeleteTag(val tag: TagUiModel) : HomeEvent()

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
