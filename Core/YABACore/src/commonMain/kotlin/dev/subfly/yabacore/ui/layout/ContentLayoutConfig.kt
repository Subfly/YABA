package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.ContentAppearance

data class GridLayoutConfig(
    val minCellWidth: Dp = 240.dp,
    val verticalSpacing: Dp = 12.dp,
    val horizontalSpacing: Dp = 12.dp,
    val outerPadding: PaddingValues = PaddingValues(0.dp),
)

data class ListLayoutConfig(
    val itemSpacing: Dp = 8.dp,
)

data class ContentLayoutConfig(
    val appearance: ContentAppearance,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val grid: GridLayoutConfig = GridLayoutConfig(),
    val list: ListLayoutConfig = ListLayoutConfig(),
)
