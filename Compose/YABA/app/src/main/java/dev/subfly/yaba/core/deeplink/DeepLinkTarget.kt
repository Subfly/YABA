package dev.subfly.yaba.core.deeplink

sealed interface DeepLinkTarget {
    data class BookmarkDetail(
        val bookmarkId: String,
        val bookmarkKindCode: Int,
    ) : DeepLinkTarget
}
