@file:OptIn(ExperimentalFoundationApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
 * - Uses [LazyColumn] internally for efficient vertical scrolling.
 * - All items are rendered as full-width list items.
 * - For grid-style bookmark layouts, use a separate composable that wraps items in a grid
 *   structure (e.g., FlowRow or custom grid) as a single item in this layout.
 *
 * ## Basic Usage:
 *
 * ```
 * YabaContentLayout(
 *   modifier = Modifier.fillMaxSize(),
 * ) {
 *   // Section header
 *   header(key = "foldersHeader") {
 *     Text("Folders", style = MaterialTheme.typography.titleMedium)
 *   }
 *
 *   // List of folders
 *   items(
 *     items = folders,
 *     key = { it.id },
 *   ) { folder ->
 *     FolderRow(folder)
 *   }
 *
 *   // Single item (e.g., a grid of bookmarks wrapped in FlowRow)
 *   header(key = "bookmarksHeader") { Text("Bookmarks") }
 *   item(key = "bookmarksGrid") {
 *     BookmarksGridSection(bookmarks)
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
 * ## Swipe Actions:
 *
 * Wrap row content with [YabaSwipeActions] for swipe-to-action functionality.
 *
 * ## Notes:
 * - Tags can still be a FlowRow/LazyRow inside an item (horizontal nesting is fine).
 * - Use stable keys for items to ensure proper recomposition and drag/drop tracking.
 * - The [listState] parameter allows external control of scroll position.
 */
@Composable
fun YabaContentLayout(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: YabaContentLayoutScope.() -> Unit,
) {
    val entries = buildList { YabaContentLayoutScopeImpl(this).content() }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        renderAsList(entries)
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
     * @param content Composable content for this item
     */
    fun item(
        key: Any,
        contentType: Any? = null,
        content: @Composable () -> Unit,
    )

    /**
     * Add a section header.
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
        content = content,
    )

    /**
     * Add multiple items from a list.
     *
     * @param items List of items to render
     * @param key Function to extract stable key from each item
     * @param contentType Optional function to extract content type from each item
     * @param itemContent Composable that receives the item
     *
     * Example with drag/drop:
     * ```
     * items(folders, key = { it.id }) { folder ->
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
        itemContent: @Composable (item: T) -> Unit,
    )
}

private class YabaContentLayoutScopeImpl(
    private val sink: MutableList<YabaContentEntry>,
) : YabaContentLayoutScope {
    override fun item(
        key: Any,
        contentType: Any?,
        content: @Composable () -> Unit,
    ) {
        sink += YabaContentEntry(
            key = key,
            contentType = contentType,
            content = content,
        )
    }

    override fun <T> items(
        items: List<T>,
        key: (T) -> Any,
        contentType: (T) -> Any?,
        itemContent: @Composable (item: T) -> Unit,
    ) {
        items.forEach { model ->
            val k = key(model)
            val ct = contentType(model)
            item(
                key = k,
                contentType = ct,
            ) { itemContent(model) }
        }
    }
}

private data class YabaContentEntry(
    val key: Any,
    val contentType: Any?,
    val content: @Composable () -> Unit,
)

private fun LazyListScope.renderAsList(
    entries: List<YabaContentEntry>,
) {
    items(
        items = entries,
        key = { it.key },
        contentType = { it.contentType },
    ) { entry ->
        Box(modifier = Modifier.animateItem()) {
            entry.content()
        }
    }
}
