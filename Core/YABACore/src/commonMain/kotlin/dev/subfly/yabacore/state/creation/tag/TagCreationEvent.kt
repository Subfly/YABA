package dev.subfly.yabacore.state.creation.tag

import dev.subfly.yabacore.model.utils.YabaColor

sealed class TagCreationEvent {
    data class OnInitWithTag(val tagIdString: String?) : TagCreationEvent()
    data class OnSelectNewColor(val newColor: YabaColor) : TagCreationEvent()
    data class OnSelectNewIcon(val newIcon: String) : TagCreationEvent()
    data class OnChangeLabel(val newLabel: String) : TagCreationEvent()
    data class OnSave(
        val onSavedCallback: () -> Unit,
        val onErrorCallback: () -> Unit,
    ): TagCreationEvent()
}
