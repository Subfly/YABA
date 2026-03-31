package dev.subfly.yaba.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal object ZoomPanUtils {
    fun clampFloat(
        value: Float,
        min: Float,
        max: Float,
    ): Float = if (value < min) min else if (value > max) max else value

    fun isFiniteOffset(offset: Offset): Boolean =
        isFiniteNumber(offset.x) && isFiniteNumber(offset.y)

    fun centerOf(size: Size): Offset = Offset(size.width / 2f, size.height / 2f)

    fun calculateOffsetForTransform(
        currentOffset: Offset,
        currentScale: Float,
        targetScale: Float,
        centroid: Offset,
        pan: Offset,
        containerSize: Size,
        minScale: Float,
        scaleEpsilon: Float,
    ): Offset {
        if (targetScale <= minScale + scaleEpsilon) return Offset.Zero

        val safeScale = if (currentScale < minScale) minScale else currentScale
        val scaleRatio = targetScale / safeScale
        val centeredCentroid = centroid - centerOf(containerSize)
        val panWithZoom = pan * targetScale

        // Keep the touched content under the fingers while scaling and panning.
        val transformedOffset = panWithZoom + (currentOffset * scaleRatio) + (centeredCentroid * (1f - scaleRatio))
        return clampOffsetToBounds(
            offset = transformedOffset,
            scale = targetScale,
            containerSize = containerSize,
            minScale = minScale,
            scaleEpsilon = scaleEpsilon,
        )
    }

    fun lerpFloat(
        start: Float,
        end: Float,
        progress: Float,
    ): Float = start + (end - start) * progress

    fun lerpOffset(
        start: Offset,
        end: Offset,
        progress: Float,
    ): Offset = Offset(
        x = lerpFloat(start.x, end.x, progress),
        y = lerpFloat(start.y, end.y, progress),
    )

    private fun isFiniteNumber(value: Float): Boolean =
        value == value && value != Float.POSITIVE_INFINITY && value != Float.NEGATIVE_INFINITY

    private fun clampOffsetToBounds(
        offset: Offset,
        scale: Float,
        containerSize: Size,
        minScale: Float,
        scaleEpsilon: Float,
    ): Offset {
        if (scale <= minScale + scaleEpsilon) return Offset.Zero

        val maxX = containerSize.width * (scale - 1f) / 2f
        val maxY = containerSize.height * (scale - 1f) / 2f
        return Offset(
            x = clampFloat(offset.x, -maxX, maxX),
            y = clampFloat(offset.y, -maxY, maxY),
        )
    }
}
