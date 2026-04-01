package dev.subfly.yaba.core.state.selection.icon

import dev.subfly.yaba.core.icons.IconSubcategory

sealed class IconSelectionEvent {
    /**
     * Load icons for [subcategory] and seed [initialSelectedIcon] as the current pick.
     */
    data class OnInit(
        val subcategory: IconSubcategory,
        val initialSelectedIcon: String,
    ) : IconSelectionEvent()

    data class OnSelectIcon(
        val iconName: String,
    ) : IconSelectionEvent()
}
