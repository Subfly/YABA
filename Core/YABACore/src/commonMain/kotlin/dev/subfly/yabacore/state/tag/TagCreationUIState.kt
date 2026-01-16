package dev.subfly.yabacore.state.tag

import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor

data class TagCreationUIState(
    val selectedColor: YabaColor = YabaColor.BLUE,
    val selectedIcon: String = "tag-01",
    val label: String = "",
    val editingTag: TagUiModel? = null,
    val isSaving: Boolean = false,
)
