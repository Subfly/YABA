@file:OptIn(ExperimentalLayoutApi::class, ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.CardImageSizing
import kotlin.uuid.ExperimentalUuidApi

/**
 * A composable that displays recent bookmarks in either a list format (for LIST/CARD appearance)
 * or a grid format (for GRID appearance).
 *
 * When [appearance] is [BookmarkAppearance.GRID], bookmarks are displayed in a [FlowRow]
 * with 3 columns. Otherwise, each bookmark is rendered as a full-width item.
 *
 * This composable should be used as a single item in [YabaContentLayout] when grid view is needed.
 *
 * @param bookmarks List of bookmarks to display
 * @param appearance The display appearance (LIST, CARD, or GRID)
 * @param cardImageSizing Image sizing for card appearance
 * @param onClickBookmark Callback when a bookmark is clicked
 * @param onDeleteBookmark Callback when a bookmark should be deleted
 * @param onShareBookmark Callback when a bookmark should be shared
 */
@Composable
fun RecentBookmarksGridSection(
    bookmarks: List<BookmarkUiModel>,
    appearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing,
    modifier: Modifier = Modifier,
    onClickBookmark: (BookmarkUiModel) -> Unit,
    onDeleteBookmark: (BookmarkUiModel) -> Unit,
    onShareBookmark: (BookmarkUiModel) -> Unit,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3,
    ) {
        bookmarks.forEachIndexed { index, bookmarkModel ->
            Box(
                modifier = Modifier.weight(1f),
            ) {
                when (bookmarkModel.kind) {
                    BookmarkKind.LINK -> {
                        BookmarkItemView(
                            model = bookmarkModel,
                            appearance = appearance,
                            cardImageSizing = cardImageSizing,
                            onClick = { onClickBookmark(bookmarkModel) },
                            onDeleteBookmark = { onDeleteBookmark(bookmarkModel) },
                            onShareBookmark = { onShareBookmark(bookmarkModel) },
                            index = index,
                            count = bookmarks.size,
                        )
                    }

                    BookmarkKind.NOTE -> {
                        // TODO: Implement NotemarkItemView
                    }

                    BookmarkKind.IMAGE -> {
                        // TODO: Implement ImagemarkItemView
                    }

                    BookmarkKind.FILE -> {
                        // TODO: Implement DocmarkItemView
                    }
                }
            }
        }
    }
}

