package dev.subfly.yaba.core.state.home

import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.HomeFolderRowUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType

data class HomeUIState(
    /** Visible folder rows for Home, already flattened and ordered. */
    val folderRows: List<HomeFolderRowUiModel> = emptyList(),
    /** Expanded folder IDs (Home-only; not persisted). */
    val expandedFolderIds: Set<String> = emptySet(),
    val tags: List<TagUiModel> = emptyList(),
    val recentBookmarks: List<BookmarkUiModel> = emptyList(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val collectionSorting: SortType = SortType.CREATED_AT,
    val sortOrder: SortOrderType = SortOrderType.ASCENDING,
    val isLoading: Boolean = true,
)
