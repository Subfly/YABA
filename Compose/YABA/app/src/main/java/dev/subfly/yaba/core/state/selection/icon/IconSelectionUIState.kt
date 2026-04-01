package dev.subfly.yaba.core.state.selection.icon

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.icons.IconItem

@Immutable
data class IconSelectionUIState(
    val icons: List<IconItem> = emptyList(),
    val isLoadingIcons: Boolean = false,
    val selectedIcon: String = "",
)
