@file:OptIn(ExperimentalFoundationApi::class)

package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Compose-side wrapper similar to SwiftUI's `swipeActions`.
 *
 * Supports up to 3 actions per side. Swipes only reveal actions; they do not auto-trigger on full
 * swipe. Provide your own action content per platform (e.g., Liquid Glass on iOS, Material on
 * Android/desktop).
 *
 * Usage (list items only):
 *
 * ```
 * YabaSwipeActions(
 *     leftActions = listOf(
 *         SwipeAction(key = "pin") { PinActionContent() }
 *     ),
 *     rightActions = listOf(
 *         SwipeAction(key = "delete", onClick = { onDelete() }) { DeleteActionContent() }
 *     ),
 * ) {
 *     /* Your list row content */
 * }
 * ```
 */
@Composable
fun YabaSwipeActions(
    modifier: Modifier = Modifier,
    leftActions: List<SwipeAction> = emptyList(),
    rightActions: List<SwipeAction> = emptyList(),
    actionWidth: Dp = 72.dp,
    actionSpacing: Dp = 6.dp,
    closeOnAction: Boolean = true,
    state: YabaSwipeState = rememberYabaSwipeState(),
    content: @Composable () -> Unit,
) {
    val left = leftActions.take(MAX_ACTIONS).also { require(leftActions.size <= MAX_ACTIONS) }
    val right = rightActions.take(MAX_ACTIONS).also { require(rightActions.size <= MAX_ACTIONS) }

    val leftWidth = remember(left, actionWidth, actionSpacing) {
        combinedWidth(left.size, actionWidth, actionSpacing)
    }
    val rightWidth = remember(right, actionWidth, actionSpacing) {
        combinedWidth(right.size, actionWidth, actionSpacing)
    }

    val density = LocalDensity.current
    val anchors = remember(leftWidth, rightWidth, density) {
        buildAnchors(leftWidth, rightWidth, density)
    }

    LaunchedEffect(anchors) { state.updateAnchors(anchors) }

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeActionsBackground(
            leftActions = left,
            rightActions = right,
            actionWidth = actionWidth,
            actionSpacing = actionSpacing,
            leftTotalWidth = leftWidth,
            rightTotalWidth = rightWidth,
        )

        Box(
            modifier = Modifier.fillMaxSize().swipeableContent(state),
        ) { content() }

        if (closeOnAction) {
            ActionClickOverlay(
                state = state,
                leftActions = left,
                rightActions = right,
                actionWidth = actionWidth,
                actionSpacing = actionSpacing,
                leftTotalWidth = leftWidth,
                rightTotalWidth = rightWidth,
            )
        }
    }
}

@Composable
private fun SwipeActionsBackground(
    leftActions: List<SwipeAction>,
    rightActions: List<SwipeAction>,
    actionWidth: Dp,
    actionSpacing: Dp,
    leftTotalWidth: Dp,
    rightTotalWidth: Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (leftActions.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(leftTotalWidth),
                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftActions.forEach { action ->
                    Box(
                        modifier = Modifier.width(actionWidth),
                        contentAlignment = Alignment.Center,
                    ) { action.content() }
                }
            }
        }
        if (rightActions.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(rightTotalWidth),
                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rightActions.forEach { action ->
                    Box(
                        modifier = Modifier.width(actionWidth),
                        contentAlignment = Alignment.Center,
                    ) { action.content() }
                }
            }
        }
    }
}

@Composable
private fun ActionClickOverlay(
    state: YabaSwipeState,
    leftActions: List<SwipeAction>,
    rightActions: List<SwipeAction>,
    actionWidth: Dp,
    actionSpacing: Dp,
    leftTotalWidth: Dp,
    rightTotalWidth: Dp,
) {
    // Hit targets that sit above background to let actions be clickable.
    Box(modifier = Modifier.fillMaxSize()) {
        val offsetPx = state.offset

        if (leftActions.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(leftTotalWidth)
                    .graphicsLayer { translationX = offsetPx.coerceAtLeast(0f) },
                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftActions.forEach { action ->
                    Box(
                        modifier = Modifier.width(actionWidth).swipeActionClick(action, state),
                        contentAlignment = Alignment.Center,
                    ) { action.content() }
                }
            }
        }

        if (rightActions.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(rightTotalWidth)
                    .graphicsLayer { translationX = offsetPx.coerceAtMost(0f) },
                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rightActions.forEach { action ->
                    Box(
                        modifier = Modifier.width(actionWidth).swipeActionClick(action, state),
                        contentAlignment = Alignment.Center,
                    ) { action.content() }
                }
            }
        }
    }
}

private fun Modifier.swipeableContent(state: YabaSwipeState): Modifier = composed {
    this.graphicsLayer { translationX = state.offset }
        .anchoredDraggable(
            state = state.draggableState,
            orientation = Orientation.Horizontal,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(state.draggableState),
        )
}

private fun Modifier.swipeActionClick(
    action: SwipeAction,
    state: YabaSwipeState,
): Modifier =
    this.then(
        composed {
            val scope = rememberCoroutineScope()
            val interaction = remember { MutableInteractionSource() }
            this.clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                action.onClick?.invoke()
                scope.launch { state.close() }
            }
        }
    )

@Stable
class YabaSwipeState
internal constructor(
    internal val draggableState: AnchoredDraggableState<SwipeValue>,
) {
    internal val offset: Float
        get() = draggableState.offset

    suspend fun close() {
        draggableState.animateTo(SwipeValue.Closed)
    }

    internal suspend fun updateAnchors(anchors: DraggableAnchors<SwipeValue>) {
        draggableState.updateAnchors(anchors)
        draggableState.snapTo(SwipeValue.Closed)
    }
}

@Composable
fun rememberYabaSwipeState(): YabaSwipeState {
    val draggable = remember {
        AnchoredDraggableState(
            initialValue = SwipeValue.Closed,
            anchors = DraggableAnchors { SwipeValue.Closed at 0f },
        )
    }
    return remember { YabaSwipeState(draggableState = draggable) }
}

@Stable
data class SwipeAction(
    val key: String,
    val onClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit,
)

internal enum class SwipeValue {
    Closed,
    LeftOpen,
    RightOpen,
}

private fun buildAnchors(
    leftWidth: Dp,
    rightWidth: Dp,
    density: Density,
): DraggableAnchors<SwipeValue> {
    return DraggableAnchors {
        SwipeValue.Closed at 0f
        val leftPx = with(density) { leftWidth.toPx() }
        val rightPx = with(density) { rightWidth.toPx() }
        if (leftPx > 0f) {
            SwipeValue.LeftOpen at leftPx
        }
        if (rightPx > 0f) {
            SwipeValue.RightOpen at -rightPx
        }
    }
}

private fun combinedWidth(count: Int, actionWidth: Dp, spacing: Dp): Dp =
    when {
        count <= 0 -> 0.dp
        count == 1 -> actionWidth
        else -> actionWidth * count + spacing * (count - 1)
    }

private const val MAX_ACTIONS = 3
