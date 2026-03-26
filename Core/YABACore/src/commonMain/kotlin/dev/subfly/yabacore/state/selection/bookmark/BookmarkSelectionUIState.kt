package dev.subfly.yabacore.state.selection.bookmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkUiModel

@Immutable
data class BookmarkSelectionUIState(
    val query: String = "",
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    val selectedBookmarkId: String? = null,
    val selectedBookmark: BookmarkUiModel? = null,
    val isLoading: Boolean = false,
)
