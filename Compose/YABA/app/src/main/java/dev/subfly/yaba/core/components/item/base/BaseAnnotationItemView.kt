@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package dev.subfly.yaba.core.components.item.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.layout.SwipeAction
import dev.subfly.yaba.layout.YabaSwipeActions
import dev.subfly.yaba.util.yabaRightClick
import dev.subfly.yaba.core.model.ui.AnnotationUiModel

/**
 * Base composable for annotation rows: shared layout (color bar + quote/note) with optional
 * swipe actions, overflow menu, and list interactions.
 *
 * When [interactive] is false, the row is display-only (no swipe, tap, or long-press).
 */
@Composable
fun BaseAnnotationItemView(
    modifier: Modifier = Modifier,
    model: AnnotationUiModel,
    menuActions: List<CollectionMenuAction> = emptyList<CollectionMenuAction>(),
    leftSwipeActions: List<CollectionSwipeAction> = emptyList<CollectionSwipeAction>(),
    rightSwipeActions: List<CollectionSwipeAction> = emptyList<CollectionSwipeAction>(),
    interactive: Boolean = true,
    onClick: () -> Unit = {},
    index: Int = 0,
    count: Int = 1,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var isMenuExpanded by remember(model.id) { mutableStateOf(false) }

    val quotePreview = remember(model.quoteText, model.note) {
        (model.quoteText ?: model.note)
            ?.trim()
            ?.take(120)
            ?.let { if (it.length >= 120) "$it…" else it }
            .orEmpty()
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
                    color = Color(action.color.iconTintArgb()),
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
                    color = Color(action.color.iconTintArgb()),
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
        if (interactive) {
            YabaSwipeActions(
                modifier = Modifier.padding(horizontal = 12.dp),
                actionWidth = 54.dp,
                leftActions = swipeLeftActions,
                rightActions = swipeRightActions,
            ) {
                AnnotationListItemRow(
                    model = model,
                    quotePreview = quotePreview,
                    index = index,
                    count = count,
                    containerColor = containerColor,
                    onClick = onClick,
                    onLongClick = { isMenuExpanded = true },
                    onRightClick = { isMenuExpanded = true },
                )
            }
        } else {
            AnnotationStaticRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                model = model,
                quotePreview = quotePreview,
                containerColor = containerColor,
            )
        }

        if (interactive) {
            AnnotationOptionsMenu(
                menuActions = menuActions,
                isExpanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
            )
        }
    }
}

@Composable
private fun AnnotationStaticRow(
    modifier: Modifier = Modifier,
    model: AnnotationUiModel,
    quotePreview: String,
    containerColor: Color,
) {
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(
                        if (itemHeightPx > 0) density.run { itemHeightPx.toDp() }
                        else 48.dp
                    )
                    .background(
                        color = Color(model.colorRole.iconTintArgb()),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { size -> itemHeightPx = size.height.toFloat() },
            ) {
                AnnotationTexts(quotePreview = quotePreview, note = model.note)
            }
        }
    }
}

@Composable
private fun AnnotationListItemRow(
    model: AnnotationUiModel,
    quotePreview: String,
    index: Int,
    count: Int,
    containerColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRightClick: () -> Unit,
) {
    var itemHeightPx by remember(model.id) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    SegmentedListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .yabaRightClick(onRightClick = onRightClick),
        onClick = onClick,
        onLongClick = onLongClick,
        colors = ListItemDefaults.colors(containerColor = containerColor),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> itemHeightPx = size.height.toFloat() },
            ) { AnnotationTexts(quotePreview = quotePreview, note = model.note) }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(
                        if (itemHeightPx > 0) density.run { itemHeightPx.toDp() }
                        else 48.dp
                    )
                    .background(
                        color = Color(model.colorRole.iconTintArgb()),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
        },
    )
}

@Composable
private fun AnnotationTexts(
    quotePreview: String,
    note: String?,
) {
    if (quotePreview.isNotBlank()) {
        Text(
            text = quotePreview,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        note?.let { n ->
            if (n.isNotBlank()) {
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = n,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnnotationOptionsMenu(
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
