@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.base

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import kotlin.uuid.ExperimentalUuidApi

/**
 * Data class representing a menu action item in the dropdown menu.
 * Used for both regular menu items and dangerous actions (like delete).
 */
@Stable
data class CollectionMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
    val isDangerous: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Data class representing a swipe action item.
 * Provides a simpler API compared to [SwipeAction] which requires composable content.
 */
@Stable
data class CollectionSwipeAction(
    val key: String,
    val icon: String,
    val color: YabaColor,
    val onClick: () -> Unit,
)

/**
 * Base composable for collection items (Folders, Tags, etc.).
 * Provides a unified UI with list appearance, swipe actions and dropdown menus.
 *
 * Note: Collections always use LIST appearance as GRID view is not supported
 * for items that can have nested children (folder-in-folder).
 *
 * @param label The primary text to display
 * @param icon The icon name to display
 * @param color The color theme for this item
 * @param parentColors List of parent colors to show as hierarchy indicators
 * @param menuActions List of menu actions to show in the dropdown menu
 * @param leftSwipeActions Swipe actions revealed when swiping right
 * @param rightSwipeActions Swipe actions revealed when swiping left
 * @param onClick Callback when the item is clicked
 * @param trailingContent Optional composable for trailing content
 * @param containerColor The background color for the list item container
 */
@Composable
fun BaseCollectionItemView(
    modifier: Modifier = Modifier,
    label: String,
    icon: String,
    color: YabaColor,
    parentColors: List<YabaColor> = emptyList(),
    menuActions: List<CollectionMenuAction> = emptyList(),
    leftSwipeActions: List<CollectionSwipeAction> = emptyList(),
    rightSwipeActions: List<CollectionSwipeAction> = emptyList(),
    onClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
    index: Int = 0,
    count: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    ListCollectionItemView(
        modifier = modifier,
        label = label,
        icon = icon,
        color = color,
        parentColors = parentColors,
        menuActions = menuActions,
        leftSwipeActions = leftSwipeActions,
        rightSwipeActions = rightSwipeActions,
        onClick = onClick,
        trailingContent = trailingContent,
        index = index,
        count = count,
        containerColor = containerColor,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ListCollectionItemView(
    modifier: Modifier,
    label: String,
    icon: String,
    color: YabaColor,
    parentColors: List<YabaColor>,
    menuActions: List<CollectionMenuAction>,
    leftSwipeActions: List<CollectionSwipeAction>,
    rightSwipeActions: List<CollectionSwipeAction>,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)?,
    index: Int,
    count: Int,
    containerColor: Color,
) {
    var isOptionsExpanded by remember { mutableStateOf(false) }
    val itemColor by remember(color) {
        mutableStateOf(Color(color.iconTintArgb()))
    }

    val swipeLeftActions = remember(leftSwipeActions) {
        leftSwipeActions.map { action ->
            SwipeAction(
                key = action.key,
                onClick = action.onClick,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(action.color.iconTintArgb())
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(12.dp),
                        name = action.icon,
                        color = Color.White,
                    )
                }
            }
        }
    }

    val swipeRightActions = remember(rightSwipeActions) {
        rightSwipeActions.map { action ->
            SwipeAction(
                key = action.key,
                onClick = action.onClick,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(action.color.iconTintArgb())
                ) {
                    YabaIcon(
                        modifier = Modifier.padding(12.dp),
                        name = action.icon,
                        color = Color.White,
                    )
                }
            }
        }
    }

    Box {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row: ColorBars (outside swipe) + YabaSwipeActions { ListItem }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Draw color bars for parent hierarchy (OUTSIDE YabaSwipeActions)
                parentColors.forEach { parentColor ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(
                                color = Color(parentColor.iconTintArgb()),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }

                // YabaSwipeActions only wraps the ListItem
                YabaSwipeActions(
                    modifier = Modifier.weight(1f),
                    actionWidth = 54.dp,
                    leftActions = swipeLeftActions,
                    rightActions = swipeRightActions,
                ) {
                    SegmentedListItem(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .yabaRightClick(onRightClick = { isOptionsExpanded = true }),
                        onClick = onClick,
                        onLongClick = { isOptionsExpanded = true },
                        colors = ListItemDefaults.colors(containerColor = containerColor),
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
                        content = { Text(label) },
                        leadingContent = { YabaIcon(name = icon, color = itemColor) },
                        trailingContent = {
                            if (trailingContent != null) {
                                trailingContent()
                            }
                        },
                    )
                }
            }
        }

        CollectionOptionsMenu(
            menuActions = menuActions,
            isExpanded = isOptionsExpanded,
            onDismissRequest = { isOptionsExpanded = false },
        )
    }
}

/**
 * Dropdown menu component that automatically groups actions.
 * Dangerous actions (like delete) are placed in a separate group at the bottom.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionOptionsMenu(
    menuActions: List<CollectionMenuAction>,
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (menuActions.isEmpty()) return

    val regularActions = remember(menuActions) {
        menuActions.filter { !it.isDangerous }
    }
    val dangerousActions = remember(menuActions) {
        menuActions.filter { it.isDangerous }
    }

    DropdownMenuPopup(
        modifier = Modifier,
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        // Regular actions group
        if (regularActions.isNotEmpty()) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = if (dangerousActions.isNotEmpty()) 2 else 1
                )
            ) {
                regularActions.forEachIndexed { index, action ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, regularActions.size),
                        checked = false,
                        onCheckedChange = { _ ->
                            onDismissRequest()
                            action.onClick()
                        },
                        leadingIcon = {
                            YabaIcon(
                                name = action.icon,
                                color = action.color,
                            )
                        },
                        text = { Text(text = action.text) }
                    )
                }
            }
        }

        // Dangerous actions group (separated with spacing)
        if (dangerousActions.isNotEmpty()) {
            if (regularActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = if (regularActions.isNotEmpty()) 1 else 0,
                    count = if (regularActions.isNotEmpty()) 2 else 1
                )
            ) {
                dangerousActions.forEachIndexed { index, action ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, dangerousActions.size),
                        checked = false,
                        onCheckedChange = { _ ->
                            onDismissRequest()
                            action.onClick()
                        },
                        leadingIcon = {
                            YabaIcon(
                                name = action.icon,
                                color = action.color,
                            )
                        },
                        text = {
                            Text(
                                text = action.text,
                                color = Color(action.color.iconTintArgb())
                            )
                        }
                    )
                }
            }
        }
    }
}
