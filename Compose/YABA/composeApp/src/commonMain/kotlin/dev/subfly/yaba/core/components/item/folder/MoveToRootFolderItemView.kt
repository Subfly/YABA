package dev.subfly.yaba.core.components.item.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.select_folder_move_to_root_label

@Composable
fun MoveToRootFolderItemView(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        headlineContent = { Text(text = stringResource(Res.string.select_folder_move_to_root_label)) },
        leadingContent = { YabaIcon(name = "arrow-move-up-right") },
    )
}
