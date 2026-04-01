package dev.subfly.yaba.core.state.selection.icon

import dev.subfly.yaba.core.icons.IconItem
import dev.subfly.yaba.core.managers.IconManager
import dev.subfly.yaba.core.state.base.BaseStateMachine
import kotlinx.coroutines.Job

class IconSelectionStateMachine :
    BaseStateMachine<IconSelectionUIState, IconSelectionEvent>(
        initialState = IconSelectionUIState(),
    ) {

    private var loadIconsJob: Job? = null

    /** Session cache so revisiting the same category does not re-read assets. */
    private val iconsByCategoryId = mutableMapOf<String, List<IconItem>>()

    override fun onEvent(event: IconSelectionEvent) {
        when (event) {
            is IconSelectionEvent.OnInit -> onInit(event)
            is IconSelectionEvent.OnSelectIcon -> onSelectIcon(event)
        }
    }

    private fun onInit(event: IconSelectionEvent.OnInit) {
        val category = event.category
        loadIconsJob?.cancel()

        updateState {
            it.copy(
                selectedIcon = event.initialSelectedIcon,
                isLoadingIcons = true,
                icons = emptyList(),
            )
        }

        val cached = iconsByCategoryId[category.id]
        if (cached != null) {
            updateState {
                it.copy(
                    icons = cached,
                    isLoadingIcons = false,
                )
            }
            return
        }

        loadIconsJob =
            launch {
                val currentJob = coroutineContext[Job]!!
                try {
                    val icons = IconManager.loadIconsForCategory(category)
                    iconsByCategoryId[category.id] = icons
                    updateState { it.copy(icons = icons, isLoadingIcons = false) }
                } finally {
                    if (loadIconsJob === currentJob) {
                        updateState { s ->
                            if (s.isLoadingIcons) s.copy(isLoadingIcons = false) else s
                        }
                    }
                }
            }
    }

    private fun onSelectIcon(event: IconSelectionEvent.OnSelectIcon) {
        updateState { it.copy(selectedIcon = event.iconName) }
    }

    fun getSelectedIcon(): String = currentState().selectedIcon

    override fun clear() {
        loadIconsJob?.cancel()
        loadIconsJob = null
        iconsByCategoryId.clear()
        super.clear()
    }
}
