package dev.subfly.yaba.core.model.annotation

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Best-effort text capture for a selection or saved annotation.
 * Used for UI preview in creation sheet and overview.
 */
@Serializable
@Stable
data class AnnotationQuoteSnapshot(
    val selectedText: String,
    val prefixText: String? = null,
    val suffixText: String? = null,
) {
    val displayText: String get() = selectedText.trim().ifBlank { "" }

    companion object {
        fun fromSelectedText(text: String): AnnotationQuoteSnapshot =
            AnnotationQuoteSnapshot(selectedText = text.trim())
    }
}
