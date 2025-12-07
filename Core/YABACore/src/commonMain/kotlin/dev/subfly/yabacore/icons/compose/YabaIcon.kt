package dev.subfly.yabacore.icons.compose

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import core.yabacore.generated.resources.Res
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Cross-platform icon renderer that will load SVG assets bundled in KMP resources.
 *
 * Icons are expected under `src/commonMain/resources/icons/<name>.svg`. If the asset is missing
 * (until icons are moved), this will render an empty box.
 */
@Composable
fun YabaIcon(
    name: String,
    color: YabaColor,
) {
    AsyncImage(
        modifier = Modifier.size(24.dp),
        model = Res.getUri("icons/${name}.svg"),
        contentDescription = name,
        colorFilter = ColorFilter.tint(color = Color.Red)
    )
}
