package dev.subfly.yaba.ui.detail.bookmark.image

import androidx.lifecycle.ViewModel
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailEvent
import dev.subfly.yabacore.state.detail.imagemark.ImagemarkDetailStateMachine

class ImagemarkDetailVM : ViewModel() {
    private val stateMachine = ImagemarkDetailStateMachine()
    val state = stateMachine.stateFlow

    fun onEvent(event: ImagemarkDetailEvent) {
        stateMachine.onEvent(event)
    }

    override fun onCleared() {
        stateMachine.clear()
        super.onCleared()
    }
}
