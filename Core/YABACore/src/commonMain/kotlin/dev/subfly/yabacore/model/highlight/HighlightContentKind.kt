package dev.subfly.yabacore.model.highlight

import kotlinx.serialization.Serializable

/**
 * Content type that can be highlighted.
 * Used to route anchors and rendering to the correct adapter.
 */
@Serializable
enum class HighlightContentKind(val code: Int) {
    READABLE(0),
    PDF(1),
    TRANSCRIPT(2),
    IMAGE(3);

    companion object {
        fun fromCode(code: Int): HighlightContentKind =
            entries.firstOrNull { it.code == code } ?: READABLE
    }
}
