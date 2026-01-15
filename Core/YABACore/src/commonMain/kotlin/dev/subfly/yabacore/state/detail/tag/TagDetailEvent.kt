package dev.subfly.yabacore.state.detail.tag

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed interface TagDetailEvent {
    data class OnInit(val tagId: String) : TagDetailEvent
    data class OnChangeQuery(val query: String) : TagDetailEvent
    data object OnToggleSelectionMode : TagDetailEvent
    data class OnToggleBookmarkSelection(val bookmarkId: Uuid) : TagDetailEvent
    data object OnDeleteSelected : TagDetailEvent
    data class OnDeleteBookmark(val bookmark: BookmarkUiModel) : TagDetailEvent
    data class OnChangeSort(val sortType: SortType, val sortOrder: SortOrderType) : TagDetailEvent
    data class OnChangeAppearance(
        val appearance: BookmarkAppearance,
        val cardImageSizing: CardImageSizing? = null
    ) : TagDetailEvent
}
