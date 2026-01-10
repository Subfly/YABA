package dev.subfly.yabacore.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing

/**
 * Configuration for bookmark appearance and sizing.
 *
 * Note: Collections (folders, tags) always use LIST appearance and don't support GRID view.
 * The [YabaContentLayout] uses LazyColumn internally. For grid-style bookmark layouts,
 * use a separate composable that wraps items in a grid structure.
 *
 * @param bookmarkAppearance Appearance for bookmark items (LIST, CARD, or GRID)
 * @param cardImageSizing Size of images in card view (only applies when bookmarkAppearance is CARD)
 * @param itemSpacing Vertical spacing between items in the list
 * @param gridColumnCount Number of columns when displaying bookmarks in GRID appearance
 */
data class ContentLayoutConfig(
    val bookmarkAppearance: BookmarkAppearance = BookmarkAppearance.LIST,
    val cardImageSizing: CardImageSizing = CardImageSizing.SMALL,
    val itemSpacing: Dp = 8.dp,
    val gridColumnCount: Int = 3,
)
