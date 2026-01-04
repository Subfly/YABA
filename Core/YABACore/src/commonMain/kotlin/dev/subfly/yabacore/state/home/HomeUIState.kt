package dev.subfly.yabacore.state.home

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

data class HomeUIState(
    val folders: List<FolderUiModel> = emptyList(),
    val tags: List<TagUiModel> = emptyList(),
    val recentBookmarks: List<BookmarkUiModel> = emptyList(),
    val contentAppearance: ContentAppearance = ContentAppearance.LIST,
    val collectionSorting: SortType = SortType.CREATED_AT,
    val sortOrder: SortOrderType = SortOrderType.ASCENDING,
    val isLoading: Boolean = true,
)
