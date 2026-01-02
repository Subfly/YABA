package dev.subfly.yabacore.state.tag

import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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