package dev.subfly.yaba.core.state.creation.tag

import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

data class TagCreationUIState(
    val selectedColor: YabaColor = YabaColor.BLUE,
    val selectedIcon: String = "tag-01",
    val label: String = "",
    val editingTag: TagUiModel? = null,
    val isSaving: Boolean = false,
)
