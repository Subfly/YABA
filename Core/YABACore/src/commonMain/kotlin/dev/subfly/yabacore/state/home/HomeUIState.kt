package dev.subfly.yabacore.state.home

import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.HomeFolderRowUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

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
