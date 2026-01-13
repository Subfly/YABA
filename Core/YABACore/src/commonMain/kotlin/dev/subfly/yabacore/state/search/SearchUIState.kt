package dev.subfly.yabacore.state.search

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Immutable
data class SearchUIState(
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val selectedFolderIds: Set<Uuid> = emptySet(),
    val selectedTagIds: Set<Uuid> = emptySet(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val sortType: SortType = SortType.EDITED_AT,
    val sortOrder: SortOrderType = SortOrderType.DESCENDING,
    val isLoading: Boolean = false,
)
