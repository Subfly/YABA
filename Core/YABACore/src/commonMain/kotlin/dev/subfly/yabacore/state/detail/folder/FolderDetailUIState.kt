package dev.subfly.yabacore.state.detail.folder

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType

@Immutable
data class FolderDetailUIState(
    val folder: FolderUiModel? = null,
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedBookmarkIds: Set<String> = emptySet(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val sortType: SortType = SortType.CREATED_AT,
    val sortOrder: SortOrderType = SortOrderType.DESCENDING,
    val isLoading: Boolean = false,
)
