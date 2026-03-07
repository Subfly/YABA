package dev.subfly.yabacore.model.highlight

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Best-effort text capture for a selection or saved highlight.
 * Used for UI preview in creation sheet and overview.
 */
@Serializable
@Stable
data class HighlightQuoteSnapshot(
    val selectedText: String,
    val prefixText: String? = null,
    val suffixText: String? = null,
) {
    val displayText: String get() = selectedText.trim().ifBlank { "" }

    companion object {
        fun fromSelectedText(text: String): HighlightQuoteSnapshot =
            HighlightQuoteSnapshot(selectedText = text.trim())
    }
}
