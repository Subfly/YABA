package dev.subfly.yaba.core.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppVM: ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()

    fun onShowCreationContent() {
        _state.update { it.copy(showCreationContent = true) }
    }

    fun onHideCreationContent() {
        _state.update { it.copy(showCreationContent = false) }
    }

    fun onShowDeletionDialog() {
        _state.update { it.copy(showDeletionDialog = true) }
    }

    fun onHideDeletionDialog() {
        _state.update { it.copy(showDeletionDialog = false) }
    }
}