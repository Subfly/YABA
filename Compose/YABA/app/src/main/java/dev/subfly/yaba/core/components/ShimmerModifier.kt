package dev.subfly.yaba.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Taken from: https://proandroiddev.com/easiest-way-to-create-a-shimmer-effect-in-jetpack-compose-b56eae5e311e
 */

/**
 * Applies a shimmer effect to the composable.
 * 
 * The shimmer effect creates an animated gradient that moves across the composable,
 * giving it a loading/shimmer appearance. This is commonly used for loading placeholders.
 * 
 * @param isLoading Whether to show the shimmer effect. When false, no shimmer is applied.
 * @param baseColor The base color for the shimmer effect. Defaults to LightGray.
 * @param highlightColor The highlight color for the shimmer effect. Defaults to White.
 * @param durationMillis The duration of one shimmer cycle in milliseconds. Defaults to 1200ms.
 * 
 * @return A [Modifier] that applies the shimmer effect when [isLoading] is true.
 * 
 * @sample
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(100.dp)
 *         .shimmer(isLoading = isLoading)
 * )
 * ```
 */
fun Modifier.shimmer(
    isLoading: Boolean = true,
    baseColor: Color = Color.LightGray.copy(alpha = 0.2f),
    highlightColor: Color = Color.LightGray.copy(alpha = 0.9f),
    durationMillis: Int = 1200,
): Modifier = composed {
    if (!isLoading) {
        return@composed this
    }

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor,
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translation",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim),
    )

    this.background(brush)
}

/**
 * A composable that wraps content with a shimmer effect when loading.
 * 
 * When [isLoading] is true, shows a shimmer effect with rounded corners.
 * When [isLoading] is false, shows the actual content.
 * 
 * @param isLoading Whether to show the shimmer effect. When false, the content is displayed.
 * @param modifier The modifier to be applied to the Box container.
 * @param cornerRadius The corner radius for the shimmer effect. Defaults to 8.dp.
 * @param content The content to display when not loading.
 * 
 * @sample
 * ```
 * ShimmerItem(isLoading = isLoading) {
 *     Text("Content")
 * }
 * ```
 */
@Composable
fun ShimmerItem(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .shimmer(isLoading = isLoading)
    ) {
        // Always render content to maintain size, but make it invisible when loading
        Box(modifier = Modifier.alpha(if (isLoading) 0f else 1f)) {
            content()
        }
    }
}

@Composable
fun ShimmerItem(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmer(isLoading = isLoading)
    ) {
        // Always render content to maintain size, but make it invisible when loading
        Box(modifier = Modifier.alpha(if (isLoading) 0f else 1f)) {
            content()
        }
    }
}
