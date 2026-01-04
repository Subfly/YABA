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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    actionWidth: Dp = 64.dp,
    actionSpacing: Dp = 0.dp,
    closeOnAction: Boolean = true,
    state: YabaSwipeState = rememberYabaSwipeState(),
    content: @Composable () -> Unit,
) {
    val left = leftActions.take(MAX_ACTIONS).also { require(leftActions.size <= MAX_ACTIONS) }
    val right = rightActions.take(MAX_ACTIONS).also { require(rightActions.size <= MAX_ACTIONS) }

    val leftWidth =
        remember(left, actionWidth, actionSpacing) {
            combinedWidth(left.size, actionWidth, actionSpacing)
        }
    val rightWidth =
        remember(right, actionWidth, actionSpacing) {
            combinedWidth(right.size, actionWidth, actionSpacing)
        }

    val density = LocalDensity.current
    val anchors =
        remember(leftWidth, rightWidth, density) {
            buildAnchors(leftWidth, rightWidth, density)
        }

    LaunchedEffect(anchors) { state.updateAnchors(anchors) }

    Box(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        // Background at the bottom - revealed when content slides away
        // Action buttons are directly clickable here
        SwipeActionsBackground(
            state = state,
            leftActions = left,
            rightActions = right,
            actionWidth = actionWidth,
            actionSpacing = actionSpacing,
            closeOnAction = closeOnAction,
        )

        // Content on top of background - slides to reveal actions
        Box(
            modifier = Modifier.fillMaxWidth().swipeableContent(state),
        ) { content() }
    }
}

@Composable
private fun BoxScope.SwipeActionsBackground(
    state: YabaSwipeState,
    leftActions: List<SwipeAction>,
    rightActions: List<SwipeAction>,
    actionWidth: Dp,
    actionSpacing: Dp,
    closeOnAction: Boolean,
) {
    val scope = rememberCoroutineScope()

    if (leftActions.isNotEmpty()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).height(actionWidth),
            horizontalArrangement =
                if (actionSpacing > 0.dp) {
                    Arrangement.spacedBy(actionSpacing)
                } else {
                    Arrangement.Start
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftActions.forEach { action ->
                Box(
                    modifier =
                        Modifier.width(actionWidth)
                            .height(actionWidth)
                            .clip(CircleShape)
                            .clickable {
                                action.onClick?.invoke()
                                if (closeOnAction) {
                                    scope.launch { state.close() }
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) { action.content() }
            }
        }
    }
    if (rightActions.isNotEmpty()) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).height(actionWidth),
            horizontalArrangement =
                if (actionSpacing > 0.dp) {
                    Arrangement.spacedBy(actionSpacing)
                } else {
                    Arrangement.Start
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rightActions.forEach { action ->
                Box(
                    modifier =
                        Modifier.width(actionWidth)
                            .height(actionWidth)
                            .clip(CircleShape)
                            .clickable {
                                action.onClick?.invoke()
                                if (closeOnAction) {
                                    scope.launch { state.close() }
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) { action.content() }
            }
        }
    }
}

private fun Modifier.swipeableContent(state: YabaSwipeState): Modifier = composed {
    this.offset { IntOffset(state.offset.roundToInt(), 0) }
        .anchoredDraggable(
            state = state.draggableState,
            orientation = Orientation.Horizontal,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(state.draggableState),
        )
}

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
