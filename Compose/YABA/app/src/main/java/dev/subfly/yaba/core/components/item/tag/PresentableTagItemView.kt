package dev.subfly.yaba.core.components.item.tag

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
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
import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.layout.SwipeAction
import dev.subfly.yaba.core.components.layout.YabaSwipeActions
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

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

    // Creation / selection sheets: system tags are only clickable — no edit swipe or overflow.
    val allowEditInteractions =
        model != null && !CoreConstants.Tag.isSystemTag(model.id)

    YabaSwipeActions(
        modifier = modifier,
        rightActions = if (allowEditInteractions) {
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
                    .then(
                        if (allowEditInteractions) {
                            Modifier.yabaRightClick(
                                onRightClick = { isOptionsExpanded = true },
                            )
                        } else {
                            Modifier
                        },
                    ),
                onClick = onPressed,
                onLongClick =
                    if (allowEditInteractions) {
                        { isOptionsExpanded = true }
                    } else {
                        null
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
                expanded = isOptionsExpanded && allowEditInteractions,
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
                            if (allowEditInteractions) {
                                onNavigateToEdit()
                            }
                        },
                        leadingIcon = { YabaIcon(name = "edit-02") },
                        text = { Text(text = stringResource(R.string.edit)) }
                    )
                }
            }
        }
    }
}