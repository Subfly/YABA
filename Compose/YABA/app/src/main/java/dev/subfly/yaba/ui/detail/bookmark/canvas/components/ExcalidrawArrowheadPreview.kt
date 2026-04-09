package dev.subfly.yaba.ui.detail.bookmark.canvas.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Compact line-ending preview for Excalidraw arrowhead keys (no bundled icons required).
 * Uses a short horizontal stub and proportionally scaled heads so shapes read clearly in small tiles.
 */
@Composable
internal fun ExcalidrawArrowheadPreview(
    arrowheadKey: String,
    color: Color,
    modifier: Modifier = Modifier,
    /** When true, mirror so the arrowhead sits on the line start (for end-cap previews). */
    flipHorizontally: Boolean = false,
) {
    val drawModifier =
        if (flipHorizontally) {
            modifier.graphicsLayer {
                scaleX = -1f
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
        } else {
            modifier
        }
    Canvas(modifier = drawModifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        // Short stub: ~22% of width — keeps focus on the cap, not a long shaft.
        val tipX = w * 0.70f
        val lineStartX = w * 0.26f
        val strokeW = max(2.2.dp.toPx(), size.minDimension * 0.055f)
        // Geometry unit scales with tile; heads stay bold in small boxes.
        val u = (size.minDimension * 0.13f).coerceIn(3.8f, 11f)

        drawLine(
            color = color,
            start = Offset(lineStartX, midY),
            end = Offset(tipX, midY),
            strokeWidth = strokeW,
        )
        val tipY = midY
        when (arrowheadKey) {
            "none" -> { /* line only */ }
            "arrow" -> {
                val wing = u * 0.95f
                val spread = u * 0.62f
                val path =
                    Path().apply {
                        moveTo(tipX - wing, tipY - spread)
                        lineTo(tipX, tipY)
                        lineTo(tipX - wing, tipY + spread)
                    }
                drawPath(path, color, style = Stroke(width = strokeW * 1.05f))
            }
            "triangle" -> {
                val depth = u * 1.05f
                val half = u * 0.72f
                val path =
                    Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(tipX - depth, tipY - half)
                        lineTo(tipX - depth, tipY + half)
                        close()
                    }
                drawPath(path, color)
            }
            "triangle_outline" -> {
                val depth = u * 1.05f
                val half = u * 0.72f
                val path =
                    Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(tipX - depth, tipY - half)
                        lineTo(tipX - depth, tipY + half)
                        close()
                    }
                drawPath(path, color, style = Stroke(width = strokeW * 1.05f))
            }
            "dot", "circle" -> {
                val r = u * 0.42f
                val cx = tipX - u * 0.55f
                drawCircle(color = color, radius = r, center = Offset(cx, tipY))
            }
            "circle_outline" -> {
                val r = u * 0.42f
                val cx = tipX - u * 0.55f
                drawCircle(
                    color = color,
                    radius = r,
                    center = Offset(cx, tipY),
                    style = Stroke(width = strokeW * 1.05f),
                )
            }
            "diamond" -> {
                val cx = tipX - u * 0.65f
                val rh = u * 0.68f
                val rw = u * 0.72f
                val path =
                    Path().apply {
                        moveTo(cx, tipY - rh)
                        lineTo(cx + rw, tipY)
                        lineTo(cx, tipY + rh)
                        lineTo(cx - rw, tipY)
                        close()
                    }
                drawPath(path, color)
            }
            "diamond_outline" -> {
                val cx = tipX - u * 0.65f
                val rh = u * 0.68f
                val rw = u * 0.72f
                val path =
                    Path().apply {
                        moveTo(cx, tipY - rh)
                        lineTo(cx + rw, tipY)
                        lineTo(cx, tipY + rh)
                        lineTo(cx - rw, tipY)
                        close()
                    }
                drawPath(path, color, style = Stroke(width = strokeW * 1.05f))
            }
            "bar" -> {
                val x = tipX - u * 0.35f
                val extent = u * 0.95f
                drawLine(
                    color = color,
                    start = Offset(x, tipY - extent),
                    end = Offset(x, tipY + extent),
                    strokeWidth = strokeW * 1.1f,
                )
            }
            "crowfoot_one" -> {
                val stemEnd = tipX - u * 0.15f
                val stemStart = tipX - u * 1.15f
                drawLine(
                    color = color,
                    start = Offset(stemStart, tipY),
                    end = Offset(stemEnd, tipY),
                    strokeWidth = strokeW,
                )
                val foot = u * 0.75f
                drawLine(
                    color = color,
                    start = Offset(tipX - u * 0.65f, tipY - foot),
                    end = Offset(tipX - u * 0.65f, tipY + foot),
                    strokeWidth = strokeW * 0.95f,
                )
            }
            "crowfoot_many" -> {
                val fan = u * 0.38f
                for (i in -1..1) {
                    val oy = i * fan
                    drawLine(
                        color = color,
                        start = Offset(tipX - u * 1.2f, tipY + oy),
                        end = Offset(tipX - u * 0.12f, tipY + oy * 0.25f),
                        strokeWidth = strokeW * 0.9f,
                    )
                }
            }
            "crowfoot_one_or_many" -> {
                drawLine(
                    color = color,
                    start = Offset(tipX - u * 1.25f, tipY),
                    end = Offset(tipX - u * 0.1f, tipY),
                    strokeWidth = strokeW,
                )
                val d = u * 0.55f
                drawLine(
                    color = color,
                    start = Offset(tipX - u * 0.75f, tipY - d),
                    end = Offset(tipX - u * 0.35f, tipY + d),
                    strokeWidth = strokeW * 0.9f,
                )
                drawLine(
                    color = color,
                    start = Offset(tipX - u * 0.75f, tipY + d),
                    end = Offset(tipX - u * 0.35f, tipY - d),
                    strokeWidth = strokeW * 0.9f,
                )
            }
            else -> {
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    radius = u * 0.28f,
                    center = Offset(tipX - u * 0.5f, tipY),
                )
            }
        }
    }
}
