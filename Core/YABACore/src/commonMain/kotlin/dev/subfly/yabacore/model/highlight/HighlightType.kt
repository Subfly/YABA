package dev.subfly.yabacore.model.highlight

/**
 * Where the highlight is anchored. Rich-text (TipTap) uses [READABLE]/[NOTE] with identity in document JSON;
 * [PDF] keeps section offsets in [dev.subfly.yabacore.database.entities.HighlightEntity.extrasJson].
 */
enum class HighlightType {
    READABLE,
    NOTE,
    PDF,
}
