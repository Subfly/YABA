package dev.subfly.yabacore.state.detail.tag

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Immutable
data class TagDetailUIState(
    val tag: TagUiModel? = null,
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedBookmarkIds: Set<Uuid> = emptySet(),
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val sortType: SortType = SortType.EDITED_AT,
    val sortOrder: SortOrderType = SortOrderType.DESCENDING,
    val isLoading: Boolean = false,
)
