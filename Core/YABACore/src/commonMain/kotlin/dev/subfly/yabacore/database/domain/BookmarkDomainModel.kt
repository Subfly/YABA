@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.database.domain

import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal sealed interface BookmarkDomainModel {
    val id: Uuid
    val folderId: Uuid
    val kind: BookmarkKind
    val label: String
    val createdAt: Instant
    val editedAt: Instant
    val viewCount: Long
    val isPrivate: Boolean
    val isPinned: Boolean
}

internal data class LinkBookmarkDomainModel(
    override val id: Uuid,
    override val folderId: Uuid,
    override val kind: BookmarkKind = BookmarkKind.LINK,
    override val label: String,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPrivate: Boolean = false,
    override val isPinned: Boolean = false,
    val description: String?,
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val previewImageUrl: String?,
    val previewIconUrl: String?,
    val videoUrl: String?,
) : BookmarkDomainModel
