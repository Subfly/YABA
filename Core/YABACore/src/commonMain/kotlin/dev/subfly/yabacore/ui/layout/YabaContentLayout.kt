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
 * ## Why this exists:
 * - Home/Sidebar often needs Tags + Folders + Bookmarks + Announcements in the same screen.
 * - Using multiple vertical lazy containers (e.g., multiple LazyColumn) can crash or behave badly.
 *
 * This composable solves that by being the ONE lazy container; you insert sections/headers/items
 * via [YabaContentLayoutScope].
 *
 * ## Basic Usage:
 *
 * ```
 * YabaContentLayout(
 *   appearance = ContentAppearance.LIST,
 *   modifier = Modifier.fillMaxSize(),
 * ) {
 *   // Section header (spans full width in grid)
 *   header(key = "foldersHeader") {
 *     Text("Folders", style = MaterialTheme.typography.titleMedium)
 *   }
 *
 *   // List of folders
 *   items(
 *     items = folders,
 *     key = { it.id },
 *   ) { folder, appearance ->
 *     FolderRow(folder, appearance)
 *   }
 *
 *   // Single item (e.g., a horizontal tag flow)
 *   header(key = "tagsHeader") { Text("Tags") }
 *   item(key = "tagsFlow") { _ ->
 *     FlowRow {
 *       tags.forEach { TagChip(it) }
 *     }
 *   }
 *
 *   // Bookmarks with different rendering per appearance
 *   header(key = "bookmarksHeader") { Text("Bookmarks") }
 *   items(
 *     items = bookmarks,
 *     key = { it.id },
 *   ) { bookmark, appearance ->
 *     when (appearance) {
 *       ContentAppearance.LIST -> BookmarkListRow(bookmark)
 *       ContentAppearance.CARD -> BookmarkCard(bookmark)
 *       ContentAppearance.GRID -> BookmarkGridCard(bookmark)
 *     }
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
 * ```
 * val dragDropState = rememberYabaDragDropState { result ->
 *   when {
 *     // Tag dragged to bookmark -> add tag to bookmark
 *     result.payload is DragTagPayload &&
 *         result.target is DragBookmarkPayload -> {
 *       TagManager.addTagToBookmark(
 *         tag = (result.payload as DragTagPayload).tag,
 *         bookmarkId = (result.target as DragBookmarkPayload).bookmark.id
 *       )
 *     }
 *     // Bookmark dragged to folder -> move bookmark
 *     result.payload is DragBookmarkPayload &&
 *         result.target is DragFolderPayload &&
 *         result.zone == DropZone.MIDDLE -> {
 *       BookmarkManager.moveBookmark(
 *         bookmark = (result.payload as DragBookmarkPayload).bookmark,
 *         targetFolder = (result.target as DragFolderPayload).folder
 *       )
 *     }
 *     // Folder reordering
 *     result.payload is DragFolderPayload &&
 *         result.target is DragFolderPayload -> {
 *       FolderManager.reorderFolder(
 *         dragged = (result.payload as DragFolderPayload).folder,
 *         target = (result.target as DragFolderPayload).folder,
 *         zone = result.zone
 *       )
 *     }
 *   }
 * }
 *
 * YabaContentLayout(appearance = appearance) {
 *   // Folders with drag/drop
 *   items(folders, key = { it.id }) { folder, appearance ->
 *     val payload = remember(folder.id) { DragFolderPayload(folder) }
 *     Box(
 *       modifier = Modifier
 *         .yabaDragSource(dragDropState, payload)
 *         .yabaDropTarget(dragDropState, payload, Orientation.Vertical)
 *     ) {
 *       FolderRow(folder, appearance, isDragging = dragDropState.isDragging(payload))
 *     }
 *   }
 *
 *   // Tags with horizontal drag/drop
 *   item(key = "tags") { _ ->
 *     YabaTagLayout(
 *       tags = tags,
 *       layoutStyle = TagLayoutStyle.Horizontal,
 *       onDrop = { /* handled by shared dragDropState */ },
 *       dragDropState = dragDropState,
 *     ) { tag, dragging ->
 *       TagChip(tag, isDragging = dragging)
 *     }
 *   }
 *
 *   // Bookmarks with drag/drop
 *   items(bookmarks, key = { it.id }) { bookmark, appearance ->
 *     val payload = remember(bookmark.id) { DragBookmarkPayload(bookmark) }
 *     Box(
 *       modifier = Modifier
 *         .yabaDragSource(dragDropState, payload)
 *         .yabaDropTarget(dragDropState, payload, Orientation.Vertical)
 *     ) {
 *       BookmarkRow(bookmark, appearance, isDragging = dragDropState.isDragging(payload))
 *     }
 *   }
 * }
 * ```
 *
 * ## Swipe Actions (List Items Only):
 *
 * Wrap row content with [YabaSwipeActions] when appearance is LIST. Swipe actions are not supported
 * for CARD or GRID appearances (use long-press context menus instead).
 *
 * ```
 * YabaContentLayout(appearance = ContentAppearance.LIST) {
 *   items(bookmarks, key = { it.id }) { bookmark, appearance ->
 *     if (appearance == ContentAppearance.LIST) {
 *       YabaSwipeActions(
 *         leftActions = listOf(
 *           SwipeAction(key = "pin", onClick = { onPin(bookmark) }) {
 *             Icon(Icons.Default.Star, "Pin")
 *           }
 *         ),
 *         rightActions = listOf(
 *           SwipeAction(key = "delete", onClick = { onDelete(bookmark) }) {
 *             Icon(Icons.Default.Delete, "Delete")
 *           }
 *         ),
 *       ) {
 *         BookmarkListRow(bookmark)
 *       }
 *     } else {
 *       BookmarkCard(bookmark)
 *     }
 *   }
 * }
 * ```
 *
 * ## Notes:
 * - Tags can still be a FlowRow/LazyRow inside an item (horizontal nesting is fine).
 * - Headers automatically span full width in grid layouts via [YabaContentSpan.FullLine].
 * - Use stable keys for items to ensure proper recomposition and drag/drop tracking.
 * - The [appearance] parameter in item lambdas lets you render differently per layout type.
 */
@Composable
fun YabaContentLayout(
    appearance: ContentAppearance,
    modifier: Modifier = Modifier,
    layoutConfig: ContentLayoutConfig = ContentLayoutConfig(appearance = appearance),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: YabaContentLayoutScope.() -> Unit,
) {
    val entries = buildList { YabaContentLayoutScopeImpl(this).content() }

    when (appearance) {
        ContentAppearance.LIST, ContentAppearance.CARD -> LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(layoutConfig.list.itemSpacing),
        ) { renderAsLazyList(entries, appearance) }

        ContentAppearance.GRID -> LazyVerticalStaggeredGrid(
            modifier = modifier,
            contentPadding = contentPadding,
            columns = StaggeredGridCells.Adaptive(layoutConfig.grid.minCellWidth),
            verticalItemSpacing = layoutConfig.grid.verticalSpacing,
            horizontalArrangement =
                Arrangement.spacedBy(layoutConfig.grid.horizontalSpacing),
        ) { renderAsStaggeredGrid(entries, appearance) }
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
     * @param content Composable that receives the current [ContentAppearance] to render accordingly
     */
    fun item(
        key: Any,
        contentType: Any? = null,
        span: YabaContentSpan = YabaContentSpan.Auto,
        content: @Composable (appearance: ContentAppearance) -> Unit,
    )

    /**
     * Add a section header (spans full width in grid layouts).
     *
     * @param key Stable key for this header
     * @param contentType Optional content type (defaults to "header")
     * @param content Header content (does not receive appearance parameter)
     */
    fun header(
        key: Any,
        contentType: Any? = "header",
        content: @Composable () -> Unit,
    ) = item(
        key = key,
        contentType = contentType,
        span = YabaContentSpan.FullLine,
    ) { _ -> content() }

    /**
     * Add multiple items from a list.
     *
     * @param items List of items to render
     * @param key Function to extract stable key from each item
     * @param contentType Optional function to extract content type from each item
     * @param span Optional function to determine span for each item in grid layouts
     * @param itemContent Composable that receives the item and current [ContentAppearance]
     *
     * Example with drag/drop:
     * ```
     * items(folders, key = { it.id }) { folder, appearance ->
     *   val payload = remember(folder.id) { DragFolderPayload(folder) }
     *   Box(
     *     modifier = Modifier
     *       .yabaDragSource(dragDropState, payload)
     *       .yabaDropTarget(dragDropState, payload, Orientation.Vertical)
     *   ) {
     *     FolderRow(folder, appearance)
     *   }
     * }
     * ```
     */
    fun <T> items(
        items: List<T>,
        key: (T) -> Any,
        contentType: (T) -> Any? = { null },
        span: (T) -> YabaContentSpan = { YabaContentSpan.Auto },
        itemContent: @Composable (item: T, appearance: ContentAppearance) -> Unit,
    )
}

/**
 * Controls how an item spans in grid layouts.
 *
 * - [Auto]: Item takes a single cell (default for most items)
 * - [FullLine]: Item spans the full width of the grid (used automatically by
 * [YabaContentLayoutScope.header])
 */
enum class YabaContentSpan {
    /** Item takes a single cell in the grid */
    Auto,

    /** Item spans the full width of the grid (useful for headers, banners, etc.) */
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
            ) { appearance -> itemContent(model, appearance) }
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
    ) { entry -> entry.content(appearance) }
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
    ) { entry -> entry.content(appearance) }
}
