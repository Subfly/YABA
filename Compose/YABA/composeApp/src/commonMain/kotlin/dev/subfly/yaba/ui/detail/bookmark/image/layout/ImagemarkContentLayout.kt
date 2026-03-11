package dev.subfly.yaba.ui.detail.bookmark.image.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.ui.detail.bookmark.image.components.ImagemarkContentDropdownMenu
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.ZoomPanUtils
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailEvent
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.image.YabaImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_title

private const val MIN_SCALE = 1F
private const val MAX_SCALE = 5F
private const val DOUBLE_TAP_SCALE = 2.5F
private const val SCALE_EPSILON = 0.001F
private const val DOUBLE_TAP_ANIMATION_DURATION_MS = 220

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ImagemarkContentLayout(
        modifier: Modifier = Modifier,
        state: ImagemarkDetailUIState,
        onShowDetail: () -> Unit,
        onEvent: (ImagemarkDetailEvent) -> Unit,
        onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val containerSizeFloat =
            remember(containerSize) {
                Size(containerSize.width.toFloat(), containerSize.height.toFloat())
            }
    val hasValidSize = containerSize.width > 0 && containerSize.height > 0

    val scope = rememberCoroutineScope()
    var doubleTapAnimationJob by remember { mutableStateOf<Job?>(null) }

    val transformableState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        doubleTapAnimationJob?.cancel()
        val targetScale = ZoomPanUtils.clampFloat(scale * zoomChange, MIN_SCALE, MAX_SCALE)

        if (!hasValidSize) {
            scale = targetScale
            offset =
                    if (targetScale <= MIN_SCALE + SCALE_EPSILON) Offset.Zero
                    else offset + panChange
            return@rememberTransformableState
        }

        val safeCentroid =
                if (ZoomPanUtils.isFiniteOffset(centroid)) centroid
                else ZoomPanUtils.centerOf(containerSizeFloat)
        offset =
                ZoomPanUtils.calculateOffsetForTransform(
                        currentOffset = offset,
                        currentScale = scale,
                        targetScale = targetScale,
                        centroid = safeCentroid,
                        pan = panChange,
                        containerSize = containerSizeFloat,
                        minScale = MIN_SCALE,
                        scaleEpsilon = SCALE_EPSILON,
                )
        scale = targetScale
    }

    Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                            title = {
                                Text(text = stringResource(Res.string.bookmark_detail_title))
                            },
                            navigationIcon = {
                                IconButton(onClick = navigator::removeLastOrNull) {
                                    YabaIcon(name = "arrow-left-01")
                                }
                            },
                            actions = {
                                IconButton(
                                        onClick = onShowDetail,
                                        shapes = IconButtonDefaults.shapes(),
                                ) { YabaIcon(name = "information-circle") }
                                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                    IconButton(
                                            onClick = { isMenuExpanded = !isMenuExpanded },
                                            shapes = IconButtonDefaults.shapes(),
                                    ) { YabaIcon(name = "more-horizontal-circle-02") }

                                    ImagemarkContentDropdownMenu(
                                            expanded = isMenuExpanded,
                                            onDismissRequest = { isMenuExpanded = false },
                                            state = state,
                                            onEvent = onEvent,
                                            onShowRemindMePicker = onShowRemindMePicker,
                                    )
                                }
                            }
                    )
                    AnimatedContent(state.isLoading) { loading ->
                        if (loading) {
                            Box(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(bottom = 4.dp)
                                                    .background(
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .surface
                                                    )
                            ) { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
    ) { paddingValues ->
        Box(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).onSizeChanged {
                            containerSize = it
                        },
                contentAlignment = Alignment.Center,
        ) {
            YabaImage(
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y,
                                    )
                                    .transformable(
                                            state = transformableState,
                                            lockRotationOnZoomPan = true,
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                                onDoubleTap = { tapOffset ->
                                                    doubleTapAnimationJob?.cancel()
                                                    doubleTapAnimationJob =
                                                            scope.launch {
                                                                val startScale = scale
                                                                val startOffset = offset
                                                                val targetScale =
                                                                        if (startScale >
                                                                                        MIN_SCALE +
                                                                                                SCALE_EPSILON
                                                                        ) {
                                                                            MIN_SCALE
                                                                        } else {
                                                                            DOUBLE_TAP_SCALE
                                                                        }

                                                                val targetOffset =
                                                                        if (!hasValidSize ||
                                                                                        targetScale <=
                                                                                                MIN_SCALE +
                                                                                                        SCALE_EPSILON
                                                                        ) {
                                                                            Offset.Zero
                                                                        } else {
                                                                            ZoomPanUtils
                                                                                    .calculateOffsetForTransform(
                                                                                            currentOffset =
                                                                                                    startOffset,
                                                                                            currentScale =
                                                                                                    startScale,
                                                                                            targetScale =
                                                                                                    targetScale,
                                                                                            centroid =
                                                                                                    if (ZoomPanUtils
                                                                                                                    .isFiniteOffset(
                                                                                                                            tapOffset
                                                                                                                    )
                                                                                                    ) {
                                                                                                        tapOffset
                                                                                                    } else {
                                                                                                        ZoomPanUtils
                                                                                                                .centerOf(
                                                                                                                        containerSizeFloat
                                                                                                                )
                                                                                                    },
                                                                                            pan =
                                                                                                    Offset.Zero,
                                                                                            containerSize =
                                                                                                    containerSizeFloat,
                                                                                            minScale =
                                                                                                    MIN_SCALE,
                                                                                            scaleEpsilon =
                                                                                                    SCALE_EPSILON,
                                                                                    )
                                                                        }

                                                                animate(
                                                                        initialValue = 0f,
                                                                        targetValue = 1f,
                                                                        animationSpec =
                                                                                tween(
                                                                                        durationMillis =
                                                                                                DOUBLE_TAP_ANIMATION_DURATION_MS,
                                                                                        easing =
                                                                                                FastOutSlowInEasing,
                                                                                ),
                                                                ) { progress, _ ->
                                                                    scale =
                                                                            ZoomPanUtils.lerpFloat(
                                                                                    startScale,
                                                                                    targetScale,
                                                                                    progress
                                                                            )
                                                                    offset =
                                                                            ZoomPanUtils.lerpOffset(
                                                                                    startOffset,
                                                                                    targetOffset,
                                                                                    progress
                                                                            )
                                                                }
                                                            }
                                                }
                                        )
                                    },
                    filePath = state.imageAbsolutePath,
            )
        }
    }
}
