package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.annotation.StringRes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.base.BaseCollectionItemView
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.TocItem

/**
 * One visible row in the lazy ToC list (tree flattened with [flattenTocForLazyItems]).
 */
internal data class TocFlattenNode(
    val item: TocItem,
    val depth: Int,
    val indexInLevel: Int,
    val siblingsCount: Int,
)

/**
 * Depth-first list of visible nodes. [collapsedIds] contains node ids whose children are hidden.
 */
internal fun flattenTocForLazyItems(
    roots: List<TocItem>,
    collapsedIds: Set<String>,
): List<TocFlattenNode> = buildList {
    fun walk(items: List<TocItem>, depth: Int) {
        items.fastForEachIndexed { i, node ->
            add(
                TocFlattenNode(
                    item = node,
                    depth = depth,
                    indexInLevel = i,
                    siblingsCount = items.size,
                ),
            )
            if (node.children.isNotEmpty() && node.id !in collapsedIds) {
                walk(node.children, depth + 1)
            }
        }
    }
    walk(roots, 0)
}

/**
 * Adds ToC content as lazy list items (one [BaseCollectionItemView] per visible row).
 * Collapse state must be hoisted in the parent composable.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.bookmarkDetailTocLazyItems(
    toc: Toc?,
    rows: List<TocFlattenNode>,
    collapsedIds: Set<String>,
    onToggleCollapse: (nodeId: String) -> Unit,
    mainColor: YabaColor,
    onItemClick: (id: String, extrasJson: String?) -> Unit,
    emptyIconName: String,
    @StringRes emptyLabelRes: Int,
    emptyMessage: @Composable () -> Unit,
) {
    if (toc == null || toc.items.isEmpty()) {
        item(key = "toc_empty") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                NoContentView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .padding(vertical = 24.dp),
                    iconName = emptyIconName,
                    labelRes = emptyLabelRes,
                    message = emptyMessage,
                )
            }
        }
    } else {
        items(
            items = rows,
            key = { row -> "${row.item.id}\u0000${row.depth}\u0000${row.indexInLevel}" },
        ) { row ->
            val node = row.item
            val parentColors = List(size = row.depth) { mainColor }
            val isExpanded = node.id !in collapsedIds
            val tint = Color(mainColor.iconTintArgb())
            val headingIcon = when (node.level.coerceIn(1, 6)) {
                1 -> "heading-01"
                2 -> "heading-02"
                3 -> "heading-03"
                4 -> "heading-04"
                5 -> "heading-05"
                6 -> "heading-06"
                else -> "heading"
            }

            BaseCollectionItemView(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .animateItem(),
                label = node.title,
                icon = headingIcon,
                color = mainColor,
                parentColors = parentColors,
                index = row.indexInLevel,
                count = row.siblingsCount,
                onClick = { onItemClick(node.id, node.extrasJson) },
                trailingContent = {
                    if (node.children.isNotEmpty()) {
                        IconButton(
                            onClick = { onToggleCollapse(node.id) },
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            YabaIcon(
                                name = if (isExpanded) "arrow-down-01" else "arrow-right-01",
                                color = tint,
                            )
                        }
                    }
                }
            )
        }
    }
}
