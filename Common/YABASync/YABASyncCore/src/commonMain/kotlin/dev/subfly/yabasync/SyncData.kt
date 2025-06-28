package dev.subfly.yabasync

import kotlinx.serialization.Serializable

@Serializable
data class SyncData(
    val id: String? = null,
    val exportedFrom: String? = null,
    val collections: List<SyncCollection>,
    val bookmarks: List<SyncBookmark>,
    val deleteLogs: List<DeleteLog>
)

@Serializable
data class SyncCollection(
    val collectionId: String,
    val label: String,
    val icon: String,
    val createdAt: String,
    val editedAt: String,
    val color: Int,
    val type: Int,
    val bookmarks: List<String>, // String IDs of stored bookmarks
    val version: Int
)

@Serializable
data class SyncBookmark(
    val bookmarkId: String? = null,
    val label: String? = null,
    val bookmarkDescription: String? = null,
    val link: String, // Only non-optional param
    val domain: String? = null,
    val createdAt: String? = null,
    val editedAt: String? = null,
    val imageUrl: String? = null,
    val iconUrl: String? = null,
    val videoUrl: String? = null,
    val readableHTML: String? = null,
    val type: Int? = null,
    val version: Int? = null,
    val imageData: String? = null, // Base64 encoded image data
    val iconData: String? = null   // Base64 encoded icon data
) 