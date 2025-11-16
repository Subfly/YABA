@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.database.migration

import kotlin.uuid.Uuid
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

data class LegacyFolder(
    val id: Uuid,
    val parentId: Uuid?,
    val label: String,
    val description: String?,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)

data class LegacyTag(
    val id: Uuid,
    val label: String,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)

data class LegacyBookmark(
    val id: Uuid,
    val folderId: Uuid,
    val label: String,
    val description: String?,
    val url: String,
    val domain: String,
    val linkTypeCode: Int,
    val createdAt: Instant,
    val editedAt: Instant,
    val previewImageUrl: String? = null,
    val previewIconUrl: String? = null,
    val videoUrl: String? = null,
)

data class LegacyTagLink(
    val tagId: Uuid,
    val bookmarkId: Uuid,
)

data class LegacySnapshot(
    val folders: List<LegacyFolder>,
    val tags: List<LegacyTag>,
    val bookmarks: List<LegacyBookmark>,
    val tagLinks: List<LegacyTagLink>,
)
