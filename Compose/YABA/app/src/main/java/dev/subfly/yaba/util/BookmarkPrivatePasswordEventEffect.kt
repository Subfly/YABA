package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.security.PrivateBookmarkPasswordEventBus

/**
 * Runs follow-up actions after a successful private-bookmark password entry from
 * [BookmarkPasswordEntrySheetContent].
 */
@Composable
fun BookmarkPrivatePasswordEventEffect(
    resolveBookmark: (String) -> BookmarkUiModel?,
    onOpenBookmark: (BookmarkUiModel) -> Unit,
    onEditBookmark: (BookmarkUiModel) -> Unit,
    onShareBookmark: (BookmarkUiModel) -> Unit,
    onDeleteBookmark: (BookmarkUiModel) -> Unit,
) {
    LaunchedEffect(Unit) {
        PrivateBookmarkPasswordEventBus.events.collect { result ->
            val id = result.bookmarkId ?: return@collect
            val model = resolveBookmark(id) ?: return@collect
            when (result.reason) {
                PrivateBookmarkPasswordReason.OPEN_BOOKMARK -> onOpenBookmark(model)
                PrivateBookmarkPasswordReason.EDIT_BOOKMARK -> onEditBookmark(model)
                PrivateBookmarkPasswordReason.SHARE_BOOKMARK -> onShareBookmark(model)
                PrivateBookmarkPasswordReason.DELETE_BOOKMARK -> onDeleteBookmark(model)
                else -> Unit
            }
        }
    }
}
