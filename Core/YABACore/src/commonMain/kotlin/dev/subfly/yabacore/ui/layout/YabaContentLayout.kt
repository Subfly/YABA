@file:OptIn(ExperimentalFoundationApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.ContentAppearance

/**
 * A single, generic content layout that can mix different item types in one place.
 *
 * Why this exists:
 * - Home/Sidebar often needs Tags + Folders + Bookmarks + Announcements in the same screen.
 * - Using multiple vertical lazy containers (e.g., multiple LazyColumn) can crash or behave badly.
 *
 * This composable solves that by being the ONE lazy container; you insert sections/headers/items via
 * [YabaContentLayoutScope].
 *
 * Notes:
 * - Tags can still be a FlowRow/LazyRow inside an item (horizontal nesting is fine).
 * - Swipe actions should be applied only in LIST appearance; you can wrap the row content with
 *   [YabaSwipeActions] inside your item lambda.
 *
 * Usage sketch:
 *
 * ```
 * YabaContentLayout(
 *   appearance = appearance,
 * ) {
 *   header(key = "foldersHeader") { FolderHeader() }
 *   items(
 *     items = folders,
 *     key = { it.id },
 *   ) { folder, appearance -> FolderRow(folder, appearance) }
 *
 *   header(key = "tagsHeader") { TagsHeader() }
 *   item(key = "tagsFlow") { appearance ->
 *     YabaTagLayout(tags, TagLayoutStyle.Flow, onDrop = onDrop, dragDropState = dragState) { tag, dragging ->
 *       TagChip(tag)
 *     }
 *   }
 *
 *   header(key = "bookmarksHeader") { BookmarkHeader() }
 *   items(
 *     items = bookmarks,
 *     key = { it.id },
 *   ) { bm, appearance ->
 *     if (appearance == ContentAppearance.LIST) {
 *       YabaSwipeActions(
 *         leftActions = ...,
 *         rightActions = ...,
 *       ) { BookmarkRow(bm) }
 *     } else {
 *       BookmarkCard(bm)
 *     }
 *   }
 * }
 * ```
 */
@Composable
fun YabaContentLayout(
    appearance: ContentAppearance,
    modifier: Modifier = Modifier,
    layoutConfig: ContentLayoutConfig = ContentLayoutConfig(appearance = appearance),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: YabaContentLayoutScope.() -> Unit,
) {
    val entries = buildList {
        YabaContentLayoutScopeImpl(this).content()
    }

    when (appearance) {
        ContentAppearance.LIST, ContentAppearance.CARD -> LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(layoutConfig.list.itemSpacing),
        ) {
            renderAsLazyList(entries, appearance)
        }

        ContentAppearance.GRID -> LazyVerticalStaggeredGrid(
            modifier = modifier,
            contentPadding = contentPadding,
            columns = StaggeredGridCells.Adaptive(layoutConfig.grid.minCellWidth),
            verticalItemSpacing = layoutConfig.grid.verticalSpacing,
            horizontalArrangement = Arrangement.spacedBy(layoutConfig.grid.horizontalSpacing),
        ) {
            renderAsStaggeredGrid(entries, appearance)
        }
    }
}

interface YabaContentLayoutScope {
    fun item(
        key: Any,
        contentType: Any? = null,
        span: YabaContentSpan = YabaContentSpan.Auto,
        content: @Composable (appearance: ContentAppearance) -> Unit,
    )

    fun header(
        key: Any,
        contentType: Any? = "header",
        content: @Composable () -> Unit,
    ) = item(
        key = key,
        contentType = contentType,
        span = YabaContentSpan.FullLine,
    ) { _ -> content() }

    fun <T> items(
        items: List<T>,
        key: (T) -> Any,
        contentType: (T) -> Any? = { null },
        span: (T) -> YabaContentSpan = { YabaContentSpan.Auto },
        itemContent: @Composable (item: T, appearance: ContentAppearance) -> Unit,
    )
}

enum class YabaContentSpan {
    Auto,
    FullLine,
}

private class YabaContentLayoutScopeImpl(
    private val sink: MutableList<YabaContentEntry>,
) : YabaContentLayoutScope {
    override fun item(
        key: Any,
        contentType: Any?,
        span: YabaContentSpan,
        content: @Composable (appearance: ContentAppearance) -> Unit,
    ) {
        sink += YabaContentEntry(
            key = key,
            contentType = contentType,
            span = span,
            content = content,
        )
    }

    override fun <T> items(
        items: List<T>,
        key: (T) -> Any,
        contentType: (T) -> Any?,
        span: (T) -> YabaContentSpan,
        itemContent: @Composable (item: T, appearance: ContentAppearance) -> Unit,
    ) {
        items.forEach { model ->
            val k = key(model)
            val ct = contentType(model)
            val sp = span(model)
            item(
                key = k,
                contentType = ct,
                span = sp,
            ) { appearance ->
                itemContent(model, appearance)
            }
        }
    }
}

private data class YabaContentEntry(
    val key: Any,
    val contentType: Any?,
    val span: YabaContentSpan,
    val content: @Composable (appearance: ContentAppearance) -> Unit,
)

private fun LazyListScope.renderAsLazyList(
    entries: List<YabaContentEntry>,
    appearance: ContentAppearance,
) {
    items(
        items = entries,
        key = { it.key },
        contentType = { it.contentType },
    ) { entry ->
        entry.content(appearance)
    }
}

private fun LazyStaggeredGridScope.renderAsStaggeredGrid(
    entries: List<YabaContentEntry>,
    appearance: ContentAppearance,
) {
    items(
        items = entries,
        key = { it.key },
        contentType = { it.contentType },
        span = { entry ->
            when (entry.span) {
                YabaContentSpan.Auto -> StaggeredGridItemSpan.SingleLane
                YabaContentSpan.FullLine -> StaggeredGridItemSpan.FullLine
            }
        }
    ) { entry ->
        entry.content(appearance)
    }
}
