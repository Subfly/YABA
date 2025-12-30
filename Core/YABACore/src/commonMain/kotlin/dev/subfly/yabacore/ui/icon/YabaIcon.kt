package dev.subfly.yabacore.ui.icon

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.yabacore.generated.resources.Res

/**
 * Cross-platform icon renderer that will load SVG assets bundled in KMP resources.
 *
 * Icons are expected under `src/commonMain/composeResources/files/icons/<name>.svg`. If the asset is missing
 * (until icons are moved), this will render an empty box.
 */
@Composable
fun YabaIcon(
    modifier: Modifier = Modifier,
    name: String,
    color: YabaColor,
) {
    val tint = when (color) {
        YabaColor.NONE -> MaterialTheme.colorScheme.primary
        else -> Color(color.iconTintArgb())
    }
    AsyncImage(
        modifier = modifier.size(24.dp),
        model = Res.getUri("files/icons/${name}.svg"),
        contentDescription = name,
        colorFilter = ColorFilter.tint(color = tint)
    )
}

@Composable
fun YabaIcon(
    modifier: Modifier = Modifier,
    name: String,
    color: Color,
) {
    AsyncImage(
        modifier = modifier.size(24.dp),
        model = Res.getUri("files/icons/${name}.svg"),
        contentDescription = name,
        colorFilter = ColorFilter.tint(color = color)
    )
}

/**
 * Theme-aware variant that chooses between [lightThemeColor] and [darkThemeColor] using
 * [isSystemInDarkTheme].
 *
 * Defaults are chosen to provide contrast (black in light theme, white in dark theme).
 */
@Composable
fun YabaIcon(
    modifier: Modifier = Modifier,
    name: String,
    lightThemeColor: Color = Color.Black,
    darkThemeColor: Color = Color.White,
) {
    val color = if (isSystemInDarkTheme()) darkThemeColor else lightThemeColor
    YabaIcon(modifier = modifier, name = name, color = color)
}
