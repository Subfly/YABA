package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.formatDateTime
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun LinkmarkDetailVersionItemContent(
    modifier: Modifier = Modifier,
    version: ReadableVersionUiModel,
    mainColor: YabaColor,
    index: Int,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var isDeleteMenuExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(version.createdAt) {
        formatDateTime(Instant.fromEpochMilliseconds(version.createdAt))
    }

    val deleteSwipeAction = remember(onDelete) {
        listOf(
            SwipeAction(
                key = "delete_version",
                onClick = onDelete,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(YabaColor.RED.iconTintArgb()),
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(12.dp),
                        name = "delete-02",
                        color = Color.White,
                    )
                }
            },
        )
    }

    Box {
        YabaSwipeActions(
            modifier = modifier.padding(horizontal = 12.dp),
            actionWidth = 54.dp,
            rightActions = deleteSwipeAction,
        ) {
            SegmentedListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .yabaRightClick(onRightClick = { isDeleteMenuExpanded = true }),
                onClick = onClick,
                onLongClick = { isDeleteMenuExpanded = true },
                colors = ListItemDefaults.colors(
                    containerColor = if (isSelected) {
                        Color(mainColor.iconTintArgb()).copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ),
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                content = { Text(formattedDate) },
                leadingContent = {
                    AnimatedContent(
                        targetState = isSelected,
                    ) { selected ->
                        if (selected) {
                            YabaIcon(
                                name = "checkmark-circle-02",
                                color = mainColor,
                            )
                        } else {
                            YabaIcon(
                                name = "clock-02",
                                color = mainColor,
                            )
                        }
                    }
                },
            )
        }

        DropdownMenuPopup(
            expanded = isDeleteMenuExpanded,
            onDismissRequest = { isDeleteMenuExpanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 1),
                    checked = false,
                    onCheckedChange = {
                        isDeleteMenuExpanded = false
                        onDelete()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "delete-02",
                            color = YabaColor.RED,
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(Res.string.delete),
                            color = Color(YabaColor.RED.iconTintArgb()),
                        )
                    },
                )
            }
        }
    }
}
