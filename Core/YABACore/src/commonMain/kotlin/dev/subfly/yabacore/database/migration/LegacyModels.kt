package dev.subfly.yabacore.database.migration

import kotlin.time.Instant

data class LegacyFolder(
    val id: String,
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)

data class LegacyTag(
    val id: String,
    val label: String,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAt: Instant,
    val editedAt: Instant,
)

data class LegacyBookmark(
    val id: String,
    val folderId: String,
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
    // Raw assets migrated from SwiftData; optional to keep snapshot size reasonable.
    val previewImageData: ByteArray? = null,
    val previewIconData: ByteArray? = null,
)

data class LegacyTagLink(
    val tagId: String,
    val bookmarkId: String,
)

data class LegacySnapshot(
    val folders: List<LegacyFolder>,
    val tags: List<LegacyTag>,
    val bookmarks: List<LegacyBookmark>,
    val tagLinks: List<LegacyTagLink>,
)
