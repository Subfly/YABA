package dev.subfly.yaba.core.navigation.alert

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeletionVM : ViewModel() {
    private val _state = MutableStateFlow<DeletionState?>(null)
    val state = _state.asStateFlow()

    fun send(newState: DeletionState) {
        _state.update { newState }
    }

    fun clear() {
        _state.update { null }
    }
}
