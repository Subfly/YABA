@file:OptIn(ExperimentalFoundationApi::class, ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.CardImageSizing
import kotlin.uuid.ExperimentalUuidApi

/** Reserve vertical space so the first grid row is not covered by the overlaid collection top bar. */
private val GridStickyTopBarPlaceholderHeight = 112.dp

private val PinnedSectionSpacerHeight = 32.dp

/**
 * Layout for displaying bookmarks with drag & drop support.
 * 
 * For LIST/CARD appearance, uses LazyColumn.
 * For GRID appearance, uses LazyVerticalStaggeredGrid with adaptive columns based on a minimum
 * cell size (see [ContentLayoutConfig.gridMinCellSize]).
 *
 * When [stickyHeaderContent] is non-null, list/card layouts use a sticky header so bookmarks scroll
 * underneath. For [BookmarkAppearance.GRID], the header is overlaid at the top of a [Box] (the
 * staggered grid API does not expose a sticky header in this stack) with a full-width leading spacer
 * so cells clear the bar.
 */
@Composable
fun YabaBookmarkLayout(
    bookmarks: List<BookmarkUiModel>,
    layoutConfig: ContentLayoutConfig,
    onDrop: (YabaDropResult) -> Unit,
    modifier: Modifier = Modifier,
    dragDropState: YabaDragDropState = rememberYabaDragDropState(onDrop),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    stickyHeaderContent: (@Composable () -> Unit)? = null,
    pinnedSectionHeader: (@Composable () -> Unit)? = null,
    pinnedBookmarks: List<BookmarkUiModel> = emptyList(),
    itemContent:
    @Composable
        (
        bookmark: BookmarkUiModel,
        isDragging: Boolean,
        appearance: BookmarkAppearance,
        cardImageSizing: CardImageSizing,
        index: Int,
        count: Int,
    ) -> Unit,
) {
    val usePinnedSection = pinnedSectionHeader != null && pinnedBookmarks.isNotEmpty()

    when (layoutConfig.bookmarkAppearance) {
        BookmarkAppearance.LIST, BookmarkAppearance.CARD ->
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                contentPadding = contentPadding,
            ) {
                stickyHeaderContent?.let { header ->
                    stickyHeader { header() }
                }
                item { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing)) }
                if (usePinnedSection) {
                    item {
                        pinnedSectionHeader()
                    }
                    itemsIndexed(
                        items = pinnedBookmarks,
                        key = { _, it -> "pinned:${it.id}:${it.label}" },
                    ) { index, bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            appearance = layoutConfig.bookmarkAppearance,
                            cardImageSizing = layoutConfig.cardImageSizing,
                            state = dragDropState,
                            orientation = Orientation.Vertical,
                            modifier = Modifier.animateItem(),
                            itemContent = itemContent,
                            index = index,
                            count = pinnedBookmarks.size,
                        )
                    }
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(PinnedSectionSpacerHeight))
                        }
                    }
                }
                itemsIndexed(
                    items = bookmarks,
                    key = { _, it -> "${it.id} ${it.label}" },
                ) { index, bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        appearance = layoutConfig.bookmarkAppearance,
                        cardImageSizing = layoutConfig.cardImageSizing,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                        index = index,
                        count = bookmarks.size,
                    )
                }
                item { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing * 2)) }
            }

        BookmarkAppearance.GRID ->
            stickyHeaderContent?.let { topBar ->
                Box(modifier = modifier) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier.padding(
                            horizontal = if (layoutConfig.gridForceApplyPadding) 12.dp else 0.dp,
                        ),
                        columns = StaggeredGridCells.Adaptive(layoutConfig.gridMinCellSize),
                        verticalItemSpacing = layoutConfig.itemSpacing,
                        horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                        contentPadding = contentPadding,
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Spacer(modifier = Modifier.height(GridStickyTopBarPlaceholderHeight))
                        }
                        item(
                            span = StaggeredGridItemSpan.FullLine,
                        ) { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing)) }
                        if (usePinnedSection) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                pinnedSectionHeader()
                            }
                            itemsIndexed(
                                items = pinnedBookmarks,
                                key = { _, item -> "pinned:${item.id}:${item.label}" },
                            ) { index, bookmark ->
                                BookmarkItem(
                                    bookmark = bookmark,
                                    appearance = layoutConfig.bookmarkAppearance,
                                    cardImageSizing = layoutConfig.cardImageSizing,
                                    state = dragDropState,
                                    orientation = Orientation.Vertical,
                                    modifier = Modifier.animateItem(),
                                    itemContent = itemContent,
                                    index = index,
                                    count = pinnedBookmarks.size,
                                )
                            }
                            if (bookmarks.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Spacer(modifier = Modifier.height(PinnedSectionSpacerHeight))
                                }
                            }
                        }
                        itemsIndexed(
                            items = bookmarks,
                            key = { _, item -> "${item.id} ${item.label}" },
                        ) { index, bookmark ->
                            BookmarkItem(
                                bookmark = bookmark,
                                appearance = layoutConfig.bookmarkAppearance,
                                cardImageSizing = layoutConfig.cardImageSizing,
                                state = dragDropState,
                                orientation = Orientation.Vertical,
                                modifier = Modifier.animateItem(),
                                itemContent = itemContent,
                                index = index,
                                count = bookmarks.size,
                            )
                        }
                        item(
                            span = StaggeredGridItemSpan.FullLine,
                        ) { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing * 2)) }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                    ) { topBar() }
                }
            } ?: LazyVerticalStaggeredGrid(
                modifier = modifier.padding(
                    horizontal = if (layoutConfig.gridForceApplyPadding) 12.dp else 0.dp,
                ),
                columns = StaggeredGridCells.Adaptive(layoutConfig.gridMinCellSize),
                verticalItemSpacing = layoutConfig.itemSpacing,
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                contentPadding = contentPadding,
            ) {
                item(
                    span = StaggeredGridItemSpan.FullLine,
                ) { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing)) }
                if (usePinnedSection) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        pinnedSectionHeader()
                    }
                    itemsIndexed(
                        items = pinnedBookmarks,
                        key = { _, item -> "pinned:${item.id}:${item.label}" },
                    ) { index, bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            appearance = layoutConfig.bookmarkAppearance,
                            cardImageSizing = layoutConfig.cardImageSizing,
                            state = dragDropState,
                            orientation = Orientation.Vertical,
                            modifier = Modifier.animateItem(),
                            itemContent = itemContent,
                            index = index,
                            count = pinnedBookmarks.size,
                        )
                    }
                    if (bookmarks.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Spacer(modifier = Modifier.height(PinnedSectionSpacerHeight))
                        }
                    }
                }
                itemsIndexed(
                    items = bookmarks,
                    key = { _, item -> "${item.id} ${item.label}" },
                ) { index, bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        appearance = layoutConfig.bookmarkAppearance,
                        cardImageSizing = layoutConfig.cardImageSizing,
                        state = dragDropState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.animateItem(),
                        itemContent = itemContent,
                        index = index,
                        count = bookmarks.size,
                    )
                }
                item(
                    span = StaggeredGridItemSpan.FullLine,
                ) { Spacer(modifier = Modifier.height(layoutConfig.headlineSpacerSizing * 2)) }
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
        index: Int,
        count: Int,
    ) -> Unit,
    index: Int,
    count: Int,
) {
    val payload = remember(bookmark.id) { DragBookmarkPayload(bookmark) }
    val isDragging = state.isDragging(payload)

    Box(
        modifier =
            modifier.yabaDropTarget(state, payload, orientation)
                .yabaDragSource(state, payload),
    ) { itemContent(bookmark, isDragging, appearance, cardImageSizing, index, count) }
}
