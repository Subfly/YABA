package dev.subfly.yaba.core.state.selection.icon

import dev.subfly.yaba.core.icons.IconCategory

sealed class IconSelectionEvent {
    /**
     * Load icons for [category] and seed [initialSelectedIcon] as the current pick.
     */
    data class OnInit(
        val category: IconCategory,
        val initialSelectedIcon: String,
    ) : IconSelectionEvent()

    data class OnSelectIcon(
        val iconName: String,
    ) : IconSelectionEvent()
}
