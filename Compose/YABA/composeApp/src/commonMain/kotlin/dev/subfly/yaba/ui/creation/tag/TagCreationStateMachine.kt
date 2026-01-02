package dev.subfly.yaba.ui.creation.tag

import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.BaseStateMachine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class TagCreationUIState(
    val selectedColor: YabaColor = YabaColor.BLUE,
    val selectedIcon: String = "tag-01",
    val label: String = "",
    val editingTag: TagUiModel? = null,
)

sealed class TagCreationEvent {
    data class OnInitWithTag(val tagIdString: String?): TagCreationEvent()
    data class OnSelectNewColor(val newColor: YabaColor): TagCreationEvent()
    data class OnSelectNewIcon(val newIcon: String): TagCreationEvent()
    data class OnChangeLabel(val newLabel: String): TagCreationEvent()
}

@OptIn(ExperimentalUuidApi::class)
class TagCreationStateMachine : BaseStateMachine<TagCreationUIState, TagCreationEvent>(
    initialState = TagCreationUIState()
) {
    override fun onEvent(event: TagCreationEvent) {
        when (event) {
            is TagCreationEvent.OnInitWithTag -> onInitWithTag(event)
            is TagCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is TagCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is TagCreationEvent.OnSelectNewIcon -> onSelectNewIcon(event)
        }
    }

    private fun onInitWithTag(event: TagCreationEvent.OnInitWithTag) {
        launch {
            event.tagIdString?.let { nonNullIdString ->
                val tagId = Uuid.parse(nonNullIdString)
                TagManager.getTag(tagId)?.let { nonNullTag ->
                    updateState {
                        it.copy(
                            editingTag = nonNullTag,
                            label = nonNullTag.label,
                            selectedColor = nonNullTag.color,
                            selectedIcon = nonNullTag.icon,
                        )
                    }
                }
            }
        }
    }

    private fun onChangeLabel(event: TagCreationEvent.OnChangeLabel) {
        updateState {
            it.copy(
                label = event.newLabel,
            )
        }
    }

    private fun onSelectNewColor(event: TagCreationEvent.OnSelectNewColor) {
        updateState {
            it.copy(
                selectedColor = event.newColor,
            )
        }
    }

    private fun onSelectNewIcon(event: TagCreationEvent.OnSelectNewIcon) {
        updateState {
            it.copy(
                selectedIcon = event.newIcon,
            )
        }
    }
}