@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface Bookmark {
    val id: Uuid
    val folderId: Uuid
    val kind: BookmarkKind
    val label: String
    val createdAt: Instant
    val editedAt: Instant
}

data class LinkBookmark(
        override val id: Uuid,
        override val folderId: Uuid,
        override val kind: BookmarkKind = BookmarkKind.LINK,
        override val label: String,
        override val createdAt: Instant,
        override val editedAt: Instant,
        val description: String?,
        val url: String,
        val domain: String,
        val linkType: LinkType,
        val previewImageUrl: String?,
        val previewIconUrl: String?,
        val videoUrl: String?,
) : Bookmark
