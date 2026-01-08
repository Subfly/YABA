package dev.subfly.yaba.core.navigation.creation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

// Taken from https://github.com/philipplackner/Nav3Guide/blob/6-nav-transition-animations/composeApp/src/commonMain/kotlin/com/plcoding/nav3_guide/navigation/ResultStore.kt
@Stable
class ResultStore {
    private val results = mutableMapOf<Any, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getResult(key: Any): T? = results[key] as? T

    fun <T> setResult(key: Any, value: T) {
        results[key] = value
    }

    fun removeResult(key: Any) {
        results.remove(key)
    }

    companion object {
        val Saver = Saver<ResultStore, Map<Any, Any?>>(
            save = { it.results.toMap() },
            restore = {
                ResultStore().apply {
                    results.putAll(it)
                }
            }
        )
    }
}

@Composable
fun rememberResultStore() = rememberSaveable(saver = ResultStore.Saver) {
    ResultStore()
}

object ResultStoreKeys {
    const val SELECTED_COLOR = "selected_color"
    const val SELECTED_ICON = "selected_icon"
    const val SELECTED_FOLDER = "selected_folder"
    const val SELECTED_TAGS = "selected_tags"
}
