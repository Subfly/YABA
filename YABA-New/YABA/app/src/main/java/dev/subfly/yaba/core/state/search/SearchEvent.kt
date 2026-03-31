package dev.subfly.yaba.core.state.search

import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

sealed interface SearchEvent {
    data object OnInit : SearchEvent
    data class OnChangeQuery(val query: String) : SearchEvent
    data class OnToggleFolderFilter(val folderId: String) : SearchEvent
    data class OnToggleTagFilter(val tagId: String) : SearchEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : SearchEvent
    data class OnChangeAppearance(
        val appearance: BookmarkAppearance,
        val cardImageSizing: CardImageSizing? = null
    ) : SearchEvent
    data class OnDeleteBookmark(val bookmark: BookmarkUiModel) : SearchEvent
}
