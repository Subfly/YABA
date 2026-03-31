package dev.subfly.yaba.core.state.detail.tag

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

@Immutable
data class TagDetailUIState(
    val tag: TagUiModel? = null,
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedBookmarkIds: Set<String> = emptySet(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val sortType: SortType = SortType.EDITED_AT,
    val sortOrder: SortOrderType = SortOrderType.DESCENDING,
    val isLoading: Boolean = false,
)
