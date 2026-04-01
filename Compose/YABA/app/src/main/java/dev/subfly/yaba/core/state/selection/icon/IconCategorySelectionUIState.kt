package dev.subfly.yaba.core.state.selection.icon

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.icons.IconCategory

@Immutable
data class IconCategorySelectionUIState(
    val categories: List<IconCategory> = emptyList(),
    val isLoading: Boolean = true,
)
