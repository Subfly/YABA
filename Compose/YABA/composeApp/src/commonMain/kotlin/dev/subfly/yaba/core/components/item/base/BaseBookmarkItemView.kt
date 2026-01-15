@file:OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.subfly.yaba.core.components.item.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.bookmark.BookmarkMenuAction
import dev.subfly.yaba.core.components.item.bookmark.BookmarkSwipeAction
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.layout.SwipeAction
import dev.subfly.yabacore.ui.layout.YabaSwipeActions
import kotlin.uuid.ExperimentalUuidApi

/**
 * Base composable for bookmark items (Links, Images, Notes, Docs).
 * Provides a unified wrapper with swipe actions and dropdown menus.
 *
 * The actual content rendering is delegated to the content composable,
 * allowing different bookmark types to define their own layouts while
 * sharing common interaction patterns.
 *
 * @param appearance The display mode (LIST, CARD, GRID)
 * @param cardImageSizing The image sizing for card view (BIG or SMALL)
 * @param menuActions List of menu actions to show in the dropdown menu
 * @param leftSwipeActions Swipe actions revealed when swiping right (only in list view)
 * @param rightSwipeActions Swipe actions revealed when swiping left (only in list view)
 * @param isOptionsExpanded Whether the dropdown menu is currently expanded
 * @param onDismissOptions Callback when the dropdown menu should be dismissed
 * @param content The main content composable for the bookmark item
 */
@Composable
fun BaseBookmarkItemView(
    modifier: Modifier = Modifier,
    appearance: BookmarkAppearance,
    cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    menuActions: List<BookmarkMenuAction> = emptyList(),
    leftSwipeActions: List<BookmarkSwipeAction> = emptyList(),
    rightSwipeActions: List<BookmarkSwipeAction> = emptyList(),
    isOptionsExpanded: Boolean,
    onDismissOptions: () -> Unit,
    content: @Composable () -> Unit,
) {
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

    Box(modifier = modifier) {
        when (appearance) {
            BookmarkAppearance.LIST -> {
                // List view has swipe actions
                YabaSwipeActions(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    actionWidth = 54.dp,
                    leftActions = swipeLeftActions,
                    rightActions = swipeRightActions,
                ) {
                    content()
                }
            }

            BookmarkAppearance.CARD, BookmarkAppearance.GRID -> {
                // Card and Grid views don't have swipe actions
                content()
            }
        }

        BookmarkOptionsMenu(
            menuActions = menuActions,
            isExpanded = isOptionsExpanded,
            onDismissRequest = onDismissOptions,
        )
    }
}

/**
 * Dropdown menu component that automatically groups actions.
 * Dangerous actions (like delete) are placed in a separate group at the bottom.
 */
@Composable
internal fun BookmarkOptionsMenu(
    menuActions: List<BookmarkMenuAction>,
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

