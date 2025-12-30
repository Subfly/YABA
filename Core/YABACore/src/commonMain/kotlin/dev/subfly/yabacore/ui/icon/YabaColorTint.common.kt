package dev.subfly.yabacore.ui.icon

import dev.subfly.yabacore.model.utils.YabaColor

/**
 * Map a logical [YabaColor] to a tint color for Compose UI.
 *
 * Implemented per-target so we can choose sensible defaults without baking the palette into
 * common code.
 */
expect fun YabaColor.iconTintArgb(): Long
