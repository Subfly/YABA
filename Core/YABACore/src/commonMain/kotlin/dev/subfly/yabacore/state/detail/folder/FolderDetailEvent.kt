package dev.subfly.yabacore.state.detail.folder

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

sealed interface FolderDetailEvent {
    data class OnInit(val folderId: String) : FolderDetailEvent
    data class OnChangeQuery(val query: String) : FolderDetailEvent
    data object OnToggleSelectionMode : FolderDetailEvent
    data class OnToggleBookmarkSelection(val bookmarkId: String) : FolderDetailEvent
    data object OnDeleteSelected : FolderDetailEvent
    data class OnDeleteBookmark(val bookmark: BookmarkUiModel) : FolderDetailEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : FolderDetailEvent
    data class OnChangeAppearance(
        val appearance: BookmarkAppearance,
        val cardImageSizing: CardImageSizing? = null
    ) : FolderDetailEvent
}
