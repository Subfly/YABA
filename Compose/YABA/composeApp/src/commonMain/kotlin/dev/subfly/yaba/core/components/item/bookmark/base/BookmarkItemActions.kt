@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yaba.core.components.item.bookmark.base

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.uuid.ExperimentalUuidApi

/**
 * Data class representing a menu action item in the dropdown menu for bookmarks.
 * Used for both regular menu items and dangerous actions (like delete).
 */
@Stable
data class BookmarkMenuAction(
    val key: String,
    val icon: String,
    val text: String,
    val color: YabaColor,
    val isDangerous: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Data class representing a swipe action item for bookmarks.
 * Provides a simpler API compared to [SwipeAction] which requires composable content.
 */
@Stable
data class BookmarkSwipeAction(
    val key: String,
    val icon: String,
    val color: YabaColor,
    val onClick: () -> Unit,
)

