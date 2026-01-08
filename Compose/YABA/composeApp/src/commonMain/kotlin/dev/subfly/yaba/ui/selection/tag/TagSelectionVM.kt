package dev.subfly.yaba.ui.selection.tag

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.state.selection.TagSelectionEvent
import dev.subfly.yabacore.state.selection.TagSelectionStateMachine
import dev.subfly.yabacore.state.selection.TagSelectionUIState

class TagSelectionVM : ViewModel() {
    private val stateMachine = TagSelectionStateMachine()
    var state = mutableStateOf(TagSelectionUIState())
        private set

    init {
        stateMachine.onState { newState ->
            state.value = newState
        }
    }

    fun onEvent(event: TagSelectionEvent) {
        stateMachine.onEvent(event)
    }

    /**
     * Returns the list of currently selected tag IDs.
     * Use this when returning to the bookmark creation screen.
     */
    fun getSelectedTagIds(): List<String> = stateMachine.getSelectedTagIds()

    /**
     * Returns the list of currently selected tags.
     * Use this when returning to the bookmark creation screen.
     */
    fun getSelectedTags(): List<TagUiModel> = stateMachine.getSelectedTags()

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}

