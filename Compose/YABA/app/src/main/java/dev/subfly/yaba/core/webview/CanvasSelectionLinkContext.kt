package dev.subfly.yaba.core.webview

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Stable
@Serializable
data class CanvasSelectionLinkContext(
    val hasSelection: Boolean = false,
    val selectedIds: List<String> = emptyList(),
    val primaryText: String = "",
    val link: String? = null,
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun parse(raw: String): CanvasSelectionLinkContext =
            runCatching { json.decodeFromString(serializer(), raw) }
                .getOrElse { CanvasSelectionLinkContext() }
    }
}
