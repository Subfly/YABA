package dev.subfly.yabacore.state.tag

import dev.subfly.yabacore.managers.TagManager
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TagCreationStateMachine : BaseStateMachine<TagCreationUIState, TagCreationEvent>(
    initialState = TagCreationUIState()
) {
    private var isInitialized = false

    override fun onEvent(event: TagCreationEvent) {
        when (event) {
            is TagCreationEvent.OnInitWithTag -> onInitWithTag(event)
            is TagCreationEvent.OnChangeLabel -> onChangeLabel(event)
            is TagCreationEvent.OnSelectNewColor -> onSelectNewColor(event)
            is TagCreationEvent.OnSelectNewIcon -> onSelectNewIcon(event)
            TagCreationEvent.OnSave -> onSave()
        }
    }

    private fun onInitWithTag(event: TagCreationEvent.OnInitWithTag) {
        if (isInitialized) return
        isInitialized = true

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

    private fun onSave() {
        val currentState = currentState()
        launch {
            if (currentState.editingTag == null) {
                TagManager.createTag(
                    TagUiModel(
                        id = Uuid.generateV7(),
                        label = currentState.label,
                        icon = currentState.selectedIcon,
                        color = currentState.selectedColor,
                        createdAt = Clock.System.now(),
                        editedAt = Clock.System.now(),
                        order = -1,
                    )
                )
            } else {
                val editingTag = currentState.editingTag
                TagManager.updateTag(
                    editingTag.copy(
                        label = if (editingTag.label != currentState.label) {
                            currentState.label
                        } else {
                            editingTag.label
                        },
                        icon = if (editingTag.icon != currentState.selectedIcon) {
                            currentState.selectedIcon
                        } else {
                            editingTag.icon
                        },
                        color = if (editingTag.color != currentState.selectedColor) {
                            currentState.selectedColor
                        } else {
                            editingTag.color
                        },
                    )
                )
            }
        }
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}