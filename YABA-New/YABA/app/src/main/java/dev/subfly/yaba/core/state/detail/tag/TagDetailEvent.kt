package dev.subfly.yaba.core.state.detail.tag

import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

sealed interface TagDetailEvent {
    data class OnInit(val tagId: String) : TagDetailEvent
    data class OnChangeQuery(val query: String) : TagDetailEvent
    data object OnToggleSelectionMode : TagDetailEvent
    data class OnToggleBookmarkSelection(val bookmarkId: String) : TagDetailEvent
    data object OnDeleteSelected : TagDetailEvent
    data class OnDeleteBookmark(val bookmark: BookmarkUiModel) : TagDetailEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : TagDetailEvent
    data class OnChangeAppearance(
        val appearance: BookmarkAppearance,
        val cardImageSizing: CardImageSizing? = null
    ) : TagDetailEvent
}
