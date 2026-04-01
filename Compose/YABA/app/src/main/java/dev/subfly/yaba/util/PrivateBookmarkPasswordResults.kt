package dev.subfly.yaba.util

import kotlinx.serialization.Serializable

@Serializable
enum class PrivateBookmarkPasswordReason {
    UNLOCK_SESSION,
    OPEN_BOOKMARK,
    TOGGLE_PRIVATE_ON,
    TOGGLE_PRIVATE_OFF,
    EDIT_BOOKMARK,
    SHARE_BOOKMARK,
    DELETE_BOOKMARK,
}

@Serializable
data class PrivateBookmarkPasswordEntryResult(
    val bookmarkId: String?,
    val reason: PrivateBookmarkPasswordReason,
)
