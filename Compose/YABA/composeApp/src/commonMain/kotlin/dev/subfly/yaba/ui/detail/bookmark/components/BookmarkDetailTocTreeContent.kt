package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.base.BaseCollectionItemView
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.webview.Toc
import dev.subfly.yabacore.webview.TocItem
import org.jetbrains.compose.resources.StringResource

@Composable
internal fun BookmarkDetailTocTreeContent(
    toc: Toc?,
    mainColor: YabaColor,
    onItemClick: (id: String, extrasJson: String?) -> Unit,
    emptyIconName: String,
    emptyLabelRes: StringResource,
    emptyMessage: @Composable () -> Unit,
) {
    if (toc == null || toc.items.isEmpty()) {
        NoContentView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .padding(vertical = 24.dp),
            iconName = emptyIconName,
            labelRes = emptyLabelRes,
            message = emptyMessage,
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            toc.items.forEachIndexed { index, item ->
                TocItemRow(
                    item = item,
                    index = index,
                    count = toc.items.size,
                    depth = 0,
                    mainColor = mainColor,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun TocItemRow(
    item: TocItem,
    index: Int,
    count: Int,
    depth: Int,
    mainColor: YabaColor,
    onItemClick: (id: String, extrasJson: String?) -> Unit,
) {
    val parentColors = List(size = depth) { mainColor }
    var expanded by remember(item.id) { mutableStateOf(true) }
    val headingIcon = when (item.level.coerceIn(1, 6)) {
        1 -> "heading-01"
        2 -> "heading-02"
        3 -> "heading-03"
        4 -> "heading-04"
        5 -> "heading-05"
        6 -> "heading-06"
        else -> "heading"
    }
    val tint = Color(mainColor.iconTintArgb())
    BaseCollectionItemView(
        label = item.title,
        icon = headingIcon,
        color = mainColor,
        parentColors = parentColors,
        index = index,
        count = count,
        onClick = { onItemClick(item.id, item.extrasJson) },
        trailingContent = {
            if (item.children.isNotEmpty()) {
                IconButton(onClick = { expanded = !expanded }) {
                    YabaIcon(
                        name = if (expanded) "arrow-down-01" else "arrow-right-01",
                        color = tint,
                    )
                }
            }
        }
    )
    if (expanded && item.children.isNotEmpty()) {
        item.children.forEachIndexed { cIndex, child ->
            TocItemRow(
                item = child,
                index = cIndex,
                count = item.children.size,
                depth = depth + 1,
                mainColor = mainColor,
                onItemClick = onItemClick,
            )
        }
    }
}
