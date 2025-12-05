package dev.subfly.yabacore.filesystem.model

enum class BookmarkFileAssetKind(val code: Int) {
    UNKNOWN(0),
    LINK_IMAGE(1),
    DOMAIN_ICON(2),
    ;

    companion object {
        fun fromCode(code: Int): BookmarkFileAssetKind =
            entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}
