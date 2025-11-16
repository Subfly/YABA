package dev.subfly.yabacore.model.utils

/** Logical kind of a bookmark. Each kind is backed by its own subtype/table pair in Room. */
enum class BookmarkKind(val code: Int) {
    LINK(0),
    NOTE(1),
    IMAGE(2),
    FILE(3);

    companion object {
        fun fromCode(code: Int): BookmarkKind = entries.firstOrNull { it.code == code } ?: LINK
    }
}
