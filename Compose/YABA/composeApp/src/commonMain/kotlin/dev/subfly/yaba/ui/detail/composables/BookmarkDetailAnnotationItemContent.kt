package dev.subfly.yaba.ui.detail.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.delete

// TODO: LOCALIZATIONS
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun BookmarkDetailAnnotationItemContent(
    modifier: Modifier = Modifier,
    annotation: AnnotationUiModel,
    index: Int,
    count: Int,
    onScrollToAnnotation: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val quotePreview = (annotation.quoteText ?: annotation.note)
        ?.trim()
        ?.take(120)
        ?.let { if (it.length >= 120) "$it…" else it }
        .orEmpty()

    val swipeRightActions = remember(onEdit, onDelete) {
        listOf(
            SwipeAction(
                key = "edit_annotation",
                onClick = onEdit,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(YabaColor.ORANGE.iconTintArgb()),
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(12.dp),
                        name = "edit-02",
                        color = Color.White,
                    )
                }
            },
            SwipeAction(
                key = "delete_annotation",
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
            rightActions = swipeRightActions,
        ) {
            SegmentedListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .yabaRightClick(onRightClick = { isMenuExpanded = true }),
                onClick = onScrollToAnnotation,
                onLongClick = { isMenuExpanded = true },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size -> itemHeightPx = size.height.toFloat() },
                    ) {
                        if (quotePreview.isNotBlank()) {
                            Text(
                                text = quotePreview,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            annotation.note?.let { note ->
                                if (note.isNotBlank()) {
                                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Annotation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(
                                if (itemHeightPx > 0) with(density) { itemHeightPx.toDp() }
                                else 48.dp
                            )
                            .background(
                                color = Color(annotation.colorRole.iconTintArgb()),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    )
                },
            )
        }

        DropdownMenuPopup(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    onCheckedChange = {
                        isMenuExpanded = false
                        onEdit()
                    },
                    leadingIcon = {
                        YabaIcon(
                            name = "edit-02",
                            color = YabaColor.ORANGE,
                        )
                    },
                    text = { Text("Edit") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    onCheckedChange = {
                        isMenuExpanded = false
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
