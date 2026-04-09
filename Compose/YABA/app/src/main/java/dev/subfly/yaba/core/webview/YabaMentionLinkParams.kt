package dev.subfly.yaba.core.webview

import androidx.core.net.toUri

private const val MENTION_PREFIX = "yaba://mention"

/**
 * Parsed query fields from `yaba://mention?...` (see web `encodeMentionUrl`).
 */
data class YabaMentionLinkParams(
    val text: String,
    val bookmarkId: String,
    val bookmarkKindCode: Int,
    val bookmarkLabel: String,
)

fun parseYabaMentionLinkParams(link: String?): YabaMentionLinkParams? {
    if (link.isNullOrBlank() || !link.startsWith(MENTION_PREFIX)) return null
    return try {
        val uri = link.toUri()
        val bookmarkId = uri.getQueryParameter("bookmarkId").orEmpty()
        if (bookmarkId.isEmpty()) return null
        YabaMentionLinkParams(
            text = uri.getQueryParameter("text").orEmpty(),
            bookmarkId = bookmarkId,
            bookmarkKindCode = uri.getQueryParameter("bookmarkKindCode")?.toIntOrNull() ?: 0,
            bookmarkLabel = uri.getQueryParameter("bookmarkLabel").orEmpty(),
        )
    } catch (_: Throwable) {
        null
    }
}
