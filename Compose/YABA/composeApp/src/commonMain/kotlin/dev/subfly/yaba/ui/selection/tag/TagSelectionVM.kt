package dev.subfly.yaba.ui.selection.tag

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.state.selection.tag.TagSelectionEvent
import dev.subfly.yabacore.state.selection.tag.TagSelectionStateMachine

class TagSelectionVM : ViewModel() {
    private val stateMachine = TagSelectionStateMachine()
    var state = stateMachine.stateFlow

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

