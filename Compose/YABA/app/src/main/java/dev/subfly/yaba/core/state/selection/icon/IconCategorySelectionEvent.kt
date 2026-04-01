package dev.subfly.yaba.core.state.selection.icon

sealed class IconCategorySelectionEvent {
    /** Load bundled category tree from assets. */
    data object OnInit : IconCategorySelectionEvent()
}
