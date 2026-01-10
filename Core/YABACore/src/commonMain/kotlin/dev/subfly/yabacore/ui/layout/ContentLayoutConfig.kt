package dev.subfly.yabacore.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing

data class GridLayoutConfig(
    val minCellWidth: Dp = 240.dp,
    val verticalSpacing: Dp = 12.dp,
    val horizontalSpacing: Dp = 12.dp,
    val outerPadding: PaddingValues = PaddingValues(0.dp),
)

data class ListLayoutConfig(
    val itemSpacing: Dp = 8.dp,
)

/**
 * Configuration for [YabaContentLayout].
 *
 * Since we now use StaggeredGrid internally for all appearances, this config
 * primarily controls spacing and sizing. The actual appearance decision is made
 * at the item level based on [bookmarkAppearance].
 *
 * Note: Collections (folders, tags) always use LIST appearance and don't support GRID view.
 *
 * @param bookmarkAppearance Appearance for bookmark items
 * @param cardImageSizing Size of images in card view (only applies when bookmarkAppearance is CARD)
 * @param grid Configuration for grid layout spacing
 * @param list Configuration for list layout spacing
 */
data class ContentLayoutConfig(
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val grid: GridLayoutConfig = GridLayoutConfig(),
    val list: ListLayoutConfig = ListLayoutConfig(),
)
