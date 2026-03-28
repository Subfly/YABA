package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> BookmarkDetailPageSegmentedRow(
    pages: List<T>,
    currentPage: T,
    onPageChange: (T) -> Unit,
    label: (T) -> String,
    iconName: (T) -> String,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item { Spacer(Modifier.width(12.dp)) }
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.wrapContentWidth(),
            ) {
                pages.fastForEachIndexed { index, page ->
                    SegmentedButton(
                        selected = currentPage == page,
                        onClick = { onPageChange(page) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = pages.size),
                        label = { Text(text = label(page)) },
                        icon = { YabaIcon(name = iconName(page)) },
                    )
                }
            }
        }
        item { Spacer(Modifier.width(12.dp)) }
    }
}
