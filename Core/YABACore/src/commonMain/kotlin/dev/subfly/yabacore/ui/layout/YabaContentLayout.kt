@file:OptIn(ExperimentalFoundationApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.BookmarkAppearance

/**
 * A single, generic content layout that can mix different item types in one place.
 *
 * ## Why this exists:
 * - Home/Sidebar often needs Tags + Folders + Bookmarks + Announcements in the same screen.
 * - Using multiple vertical lazy containers (e.g., multiple LazyColumn) can crash or behave badly.
 *
 * This composable solves that by being the ONE lazy container; you insert sections/headers/items
 * via [YabaContentLayoutScope].
 *
 * ## Layout Behavior:
 * - Uses [LazyVerticalStaggeredGrid] internally for all appearances.
 * - Collections (folders, tags) always use LIST appearance and span full width.
 * - When [BookmarkAppearance.LIST]/[BookmarkAppearance.CARD],
 *   items should use [YabaContentSpan.FullLine] to span full width (behaves like LazyColumn).
 * - When [BookmarkAppearance.GRID], bookmark items use single cells.
 *
 * ## Basic Usage:
 *
 * ```
 * YabaContentLayout(
 *   layoutConfig = ContentLayoutConfig(
 *     bookmarkAppearance = BookmarkAppearance.CARD,
 *   ),
 *   modifier = Modifier.fillMaxSize(),
 * ) {
 *   // Section header (always spans full width)
 *   header(key = "foldersHeader") {
 *     Text("Folders", style = MaterialTheme.typography.titleMedium)
 *   }
 *
 *   // List of folders - use FullLine span when collection is LIST
 *   items(
 *     items = folders,
 *     key = { it.id },
 *     span = { YabaContentSpan.FullLine }, // For LIST appearance
 *   ) { folder ->
 *     FolderRow(folder)
 *   }
 *
 *   // Single item (e.g., a horizontal tag flow)
 *   header(key = "tagsHeader") { Text("Tags") }
 *   item(key = "tagsFlow") {
 *     FlowRow {
 *       tags.forEach { TagChip(it) }
 *     }
 *   }
 *
 *   // Bookmarks - span depends on bookmark appearance
 *   header(key = "bookmarksHeader") { Text("Bookmarks") }
 *   items(
 *     items = bookmarks,
 *     key = { it.id },
 *     span = { if (bookmarkAppearance == BookmarkAppearance.GRID) YabaContentSpan.Auto else YabaContentSpan.FullLine },
 *   ) { bookmark ->
 *     BookmarkItem(bookmark, bookmarkAppearance)
 *   }
 * }
 * ```
 *
 * ## Drag & Drop Integration:
 *
 * Share a single [YabaDragDropState] across all items to enable cross-type drops (e.g., tag ->
 * bookmark, bookmark -> folder). Apply [yabaDragSource] and [yabaDropTarget] modifiers inside your
 * item content.
 *
 * ## Swipe Actions (List Items Only):
 *
 * Wrap row content with [YabaSwipeActions] when appearance is LIST. Swipe actions are not supported
 * for CARD or GRID appearances (use long-press context menus instead).
 *
 * ## Notes:
 * - Tags can still be a FlowRow/LazyRow inside an item (horizontal nesting is fine).
 * - Headers automatically span full width in grid layouts via [YabaContentSpan.FullLine].
 * - Use stable keys for items to ensure proper recomposition and drag/drop tracking.
 * - Item composables no longer receive appearance - access it from state/config directly.
 */
@Composable
fun YabaContentLayout(
    layoutConfig: ContentLayoutConfig,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: YabaContentLayoutScope.() -> Unit,
) {
    val entries = buildList { YabaContentLayoutScopeImpl(this).content() }

    LazyVerticalStaggeredGrid(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr) +
                layoutConfig.grid.outerPadding.calculateLeftPadding(LayoutDirection.Ltr),
            end = contentPadding.calculateRightPadding(LayoutDirection.Ltr) +
                layoutConfig.grid.outerPadding.calculateRightPadding(LayoutDirection.Ltr),
            top = contentPadding.calculateTopPadding() +
                layoutConfig.grid.outerPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() +
                layoutConfig.grid.outerPadding.calculateBottomPadding(),
        ),
        columns = StaggeredGridCells.Adaptive(layoutConfig.grid.minCellWidth),
        verticalItemSpacing = layoutConfig.grid.verticalSpacing,
        horizontalArrangement = Arrangement.spacedBy(layoutConfig.grid.horizontalSpacing),
    ) {
        renderAsStaggeredGrid(entries)
    }
}

/**
 * Scope for building mixed content layouts.
 *
 * Use [header] for section titles, [item] for single items, and [items] for lists. All methods
 * support drag/drop by applying [yabaDragSource] / [yabaDropTarget] inside your content.
 */
interface YabaContentLayoutScope {
    /**
     * Add a single item to the layout.
     *
     * @param key Stable key for this item (used for recomposition and drag/drop tracking)
     * @param contentType Optional content type hint for performance optimization
     * @param span How this item spans in grid layouts (Auto = single cell, FullLine = full width)
     * @param content Composable content for this item
     */
    fun item(
        key: Any,
        contentType: Any? = null,
        span: YabaContentSpan = YabaContentSpan.Auto,
        content: @Composable () -> Unit,
    )

    /**
     * Add a section header (spans full width in grid layouts).
     *
     * @param key Stable key for this header
     * @param contentType Optional content type (defaults to "header")
     * @param content Header content
     */
    fun header(
        key: Any,
        contentType: Any? = "header",
        content: @Composable () -> Unit,
    ) = item(
        key = key,
        contentType = contentType,
        span = YabaContentSpan.FullLine,
        content = content,
    )

    /**
     * Add multiple items from a list.
     *
     * @param items List of items to render
     * @param key Function to extract stable key from each item
     * @param contentType Optional function to extract content type from each item
     * @param span Optional function to determine span for each item in grid layouts
     * @param itemContent Composable that receives the item
     *
     * Example with drag/drop:
     * ```
     * items(folders, key = { it.id }, span = { YabaContentSpan.FullLine }) { folder ->
     *   val payload = remember(folder.id) { DragFolderPayload(folder) }
     *   Box(
     *     modifier = Modifier
     *       .yabaDragSource(dragDropState, payload)
     *       .yabaDropTarget(dragDropState, payload, Orientation.Vertical)
     *   ) {
     *     FolderRow(folder)
     *   }
     * }
     * ```
     */
    fun <T> items(
        items: List<T>,
        key: (T) -> Any,
        contentType: (T) -> Any? = { null },
        span: (T) -> YabaContentSpan = { YabaContentSpan.Auto },
        itemContent: @Composable (item: T) -> Unit,
    )
}

/**
 * Controls how an item spans in grid layouts.
 *
 * - [Auto]: Item takes a single cell (default for grid items)
 * - [FullLine]: Item spans the full width of the grid (used for headers, list items, card items)
 */
enum class YabaContentSpan {
    /** Item takes a single cell in the grid */
    Auto,

    /** Item spans the full width of the grid (useful for headers, list/card items, banners, etc.) */
    FullLine,
}

private class YabaContentLayoutScopeImpl(
    private val sink: MutableList<YabaContentEntry>,
) : YabaContentLayoutScope {
    override fun item(
        key: Any,
        contentType: Any?,
        span: YabaContentSpan,
        content: @Composable () -> Unit,
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
        itemContent: @Composable (item: T) -> Unit,
    ) {
        items.forEach { model ->
            val k = key(model)
            val ct = contentType(model)
            val sp = span(model)
            item(
                key = k,
                contentType = ct,
                span = sp,
            ) { itemContent(model) }
        }
    }
}

private data class YabaContentEntry(
    val key: Any,
    val contentType: Any?,
    val span: YabaContentSpan,
    val content: @Composable () -> Unit,
)

private fun LazyStaggeredGridScope.renderAsStaggeredGrid(
    entries: List<YabaContentEntry>,
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
    ) { entry -> entry.content() }
}
