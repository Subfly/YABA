package dev.subfly.yabacore.model

/** User-facing categorization for link bookmarks. Mirrors Darwin's BookmarkType. */
enum class LinkType(val code: Int) {
    NONE(1),
    WEB_LINK(2),
    VIDEO(3),
    IMAGE(4),
    AUDIO(5),
    MUSIC(6);

    companion object {
        fun fromCode(code: Int): LinkType = entries.firstOrNull { it.code == code } ?: NONE
    }
}
