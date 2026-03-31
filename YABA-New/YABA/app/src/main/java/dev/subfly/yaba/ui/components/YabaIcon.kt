package dev.subfly.yaba.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.util.BundleReader
import dev.subfly.yaba.core.util.SvgImageLoader

/** Renders HugeIcons stroke SVGs from `assets/files/icons/<name>.svg` via Coil + SvgDecoder. */
@Composable
fun YabaIcon(
    modifier: Modifier = Modifier,
    name: String,
    color: YabaColor,
) {
    val tint = when (color) {
        YabaColor.NONE -> MaterialTheme.colorScheme.primary
        else -> Color((color.iconTintArgb() and 0xFFFFFFFFL).toInt())
    }

    AsyncImage(
        modifier = modifier.size(24.dp),
        model = BundleReader.getIconUri(name),
        contentDescription = name,
        imageLoader = SvgImageLoader.imageLoader,
        colorFilter = ColorFilter.tint(color = tint),
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
        model = BundleReader.getIconUri(name),
        contentDescription = name,
        imageLoader = SvgImageLoader.imageLoader,
        colorFilter = ColorFilter.tint(color = color),
    )
}

/**
 * Theme-aware variant that chooses between [lightThemeColor] and [darkThemeColor] using
 * [isSystemInDarkTheme].
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
