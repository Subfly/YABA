package dev.subfly.yaba.core.state.selection.icon

import dev.subfly.yaba.core.managers.IconManager
import dev.subfly.yaba.core.state.base.BaseStateMachine

class IconCategorySelectionStateMachine :
    BaseStateMachine<IconCategorySelectionUIState, IconCategorySelectionEvent>(
        initialState = IconCategorySelectionUIState(),
    ) {

    private var isInitialized = false

    override fun onEvent(event: IconCategorySelectionEvent) {
        when (event) {
            IconCategorySelectionEvent.OnInit -> onInit()
        }
    }

    private fun onInit() {
        if (isInitialized) return
        isInitialized = true
        launch {
            updateState { it.copy(isLoading = true) }
            val categories = IconManager.loadAllCategories()
            updateState {
                it.copy(
                    categories = categories,
                    isLoading = false,
                )
            }
        }
    }

    override fun clear() {
        isInitialized = false
        super.clear()
    }
}
