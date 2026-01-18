package dev.subfly.yabacore.state.search

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

@Immutable
data class SearchUIState(
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val selectedFolderIds: Set<String> = emptySet(),
    val selectedTagIds: Set<String> = emptySet(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val sortType: SortType = SortType.EDITED_AT,
    val sortOrder: SortOrderType = SortOrderType.DESCENDING,
    val isLoading: Boolean = false,
)
