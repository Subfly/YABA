package dev.subfly.yaba.core.components.item.tag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.edit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresentableTagItemView(
    modifier: Modifier = Modifier,
    model: TagUiModel?,
    nullModelPresentableColor: YabaColor,
    onPressed: () -> Unit,
    onNavigateToEdit: () -> Unit,
    cornerSize: Dp = 24.dp,
    index: Int = 0,
    count: Int = 1,
) {
    var isOptionsExpanded by remember { mutableStateOf(false) }

    YabaSwipeActions(
        modifier = modifier,
        rightActions = if (model != null) {
            listOf(
                SwipeAction(
                    key = "EDIT",
                    onClick = onNavigateToEdit,
                    content = {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(YabaColor.ORANGE.iconTintArgb())
                        ) {
                            YabaIcon(
                                modifier = Modifier.padding(12.dp),
                                name = "edit-02",
                                color = Color.White,
                            )
                        }
                    }
                )
            )
        } else emptyList()
    ) {
        Box {
            SegmentedListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerSize))
                    .yabaRightClick(
                        onRightClick = {
                            if (model != null) {
                                isOptionsExpanded = true
                            }
                        }
                    ),
                onClick = onPressed,
                onLongClick = {
                    if (model != null) {
                        isOptionsExpanded = true
                    }
                },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                content = { Text(text = model?.label ?: "") },
                leadingContent = {
                    YabaIcon(
                        name = model?.icon ?: "tag-01",
                        color = model?.color ?: nullModelPresentableColor,
                    )
                },
            )
            DropdownMenuPopup(
                modifier = modifier,
                expanded = isOptionsExpanded,
                onDismissRequest = { isOptionsExpanded = false },
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(
                        index = 0,
                        count = 1
                    )
                ) {
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(0, 1),
                        checked = false,
                        onCheckedChange = { _ ->
                            isOptionsExpanded = false
                            onNavigateToEdit()
                        },
                        leadingIcon = { YabaIcon(name = "edit-02") },
                        text = { Text(text = stringResource(Res.string.edit)) }
                    )
                }
            }
        }
    }
}