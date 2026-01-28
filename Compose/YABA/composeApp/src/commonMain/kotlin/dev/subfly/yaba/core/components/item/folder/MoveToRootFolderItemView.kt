package dev.subfly.yaba.core.components.item.folder

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.select_folder_move_to_root_label

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoveToRootFolderItemView(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    index: Int = 0,
    count: Int = 1,
) {
    SegmentedListItem(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = { Text(text = stringResource(Res.string.select_folder_move_to_root_label)) },
        leadingContent = { YabaIcon(name = "arrow-move-up-right") },
    )
}
