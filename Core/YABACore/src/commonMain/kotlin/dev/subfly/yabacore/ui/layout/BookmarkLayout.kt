@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import kotlin.uuid.ExperimentalUuidApi

/**
 * Layout for displaying bookmarks with drag & drop support.
 * 
 * For LIST/CARD appearance, uses LazyColumn.
 * For GRID appearance, uses LazyVerticalStaggeredGrid with adaptive columns based on a minimum
 * cell size (see [ContentLayoutConfig.gridMinCellSize]).
 */
@Composable
fun YabaBookmarkLayout(
    bookmarks: List<BookmarkUiModel>,
    layoutConfig: ContentLayoutConfig,
    onDrop: (YabaDropResult) -> Unit,
    modifier: Modifier = Modifier,
    dragDropState: YabaDragDropState = rememberYabaDragDropState(onDrop),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent:
    @Composable
        (
        bookmark: BookmarkUiModel,
        isDragging: Boolean,
        appearance: BookmarkAppearance,
        cardImageSizing: CardImageSizing,
    ) -> Unit,
) {
    when (layoutConfig.bookmarkAppearance) {
        BookmarkAppearance.LIST, BookmarkAppearance.CARD ->
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                contentPadding = contentPadding,
            ) {
                item { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing)) }
                items(
                    items = bookmarks,
                    key = { "${it.id} ${it.label}" },
                ) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        appearance = layoutConfig.bookmarkAppearance,
                        cardImageSizing = layoutConfig.cardImageSizing,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                    )
                }
            }

        BookmarkAppearance.GRID ->
            LazyVerticalStaggeredGrid(
                modifier = modifier,
                columns = StaggeredGridCells.Adaptive(layoutConfig.gridMinCellSize),
                verticalItemSpacing = layoutConfig.itemSpacing,
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                contentPadding = contentPadding,
            ) {
                item(
                    span = StaggeredGridItemSpan.FullLine,
                ) { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing)) }
                items(
                    items = bookmarks,
                    key = { "${it.id} ${it.label}" },
                ) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        appearance = layoutConfig.bookmarkAppearance,
                        cardImageSizing = layoutConfig.cardImageSizing,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                    )
                }
            }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: BookmarkUiModel,
    appearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing,
    state: YabaDragDropState,
    orientation: Orientation,
    modifier: Modifier = Modifier,
    itemContent:
    @Composable
        (
        bookmark: BookmarkUiModel,
        isDragging: Boolean,
        appearance: BookmarkAppearance,
        cardImageSizing: CardImageSizing,
    ) -> Unit,
) {
    val payload = remember(bookmark.id) { DragBookmarkPayload(bookmark) }
    val isDragging = state.isDragging(payload)

    Box(
        modifier =
            modifier.yabaDropTarget(state, payload, orientation)
                .yabaDragSource(state, payload),
    ) { itemContent(bookmark, isDragging, appearance, cardImageSizing) }
}
