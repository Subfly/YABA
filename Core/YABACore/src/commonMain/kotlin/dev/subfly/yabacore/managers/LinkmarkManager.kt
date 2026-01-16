@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import io.github.vinceglb.filekit.path
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Filesystem-first link bookmark manager.
 *
 * Handles link-specific data stored in `/bookmarks/<uuid>/link.json`.
 * Base bookmark metadata is handled by [AllBookmarksManager].
 */
object LinkmarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System

    // ==================== Query Operations ====================

    suspend fun getLinkmarkDetail(bookmarkId: Uuid): LinkmarkUiModel? {
        val linkBookmark = bookmarkDao.getLinkBookmarkById(bookmarkId.toString()) ?: return null
        val domain = linkBookmark.toModel()
        val folder = folderDao.getFolderWithBookmarkCount(domain.folderId.toString())?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId.toString()).map { it.toUiModel() }

        val localImageAbsolutePath =
            domain.localImagePath?.let { relativePath -> BookmarkFileManager.resolve(relativePath).path }
        val localIconAbsolutePath =
            domain.localIconPath?.let { relativePath -> BookmarkFileManager.resolve(relativePath).path }

        return domain.toUiModel(
            folder = folder,
            tags = tags,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
        )
    }

    // ==================== Write Operations (Filesystem-First) ====================

    /**
     * Creates link-specific details (url/domain/linkType/videoUrl) for a bookmark.
     *
     * Bookmark metadata (folder/title/description/tags/preview assets) must be saved via
     * [AllBookmarksManager] first.
     */
    suspend fun createLinkDetails(
        bookmarkId: Uuid,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)
        val resolvedDomain = domain?.takeIf { it.isNotBlank() } ?: extractDomain(url)

        val linkJson = LinkJson(
            url = url,
            domain = resolvedDomain,
            linkTypeCode = linkType.code,
            videoUrl = videoUrl,
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeLinkJson(bookmarkId, linkJson)

        // 2. Record CRDT events
        recordLinkCreationEvents(bookmarkId, linkJson)

        // 3. Update SQLite cache
        linkBookmarkDao.upsert(linkJson.toEntity(bookmarkId))
    }

    /**
     * Updates link-specific details for an existing link bookmark.
     */
    suspend fun updateLinkDetails(
        bookmarkId: Uuid,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        val existingJson = entityFileManager.readLinkJson(bookmarkId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val deviceId = DeviceIdProvider.get()
        val resolvedDomain = domain?.takeIf { it.isNotBlank() } ?: extractDomain(url)

        // Detect changes
        val changes = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        if (existingJson?.url != url) {
            changes["url"] = JsonPrimitive(url)
        }
        if (existingJson?.domain != resolvedDomain) {
            changes["domain"] = JsonPrimitive(resolvedDomain)
        }
        if (existingJson?.linkTypeCode != linkType.code) {
            changes["linkTypeCode"] = JsonPrimitive(linkType.code)
        }
        if (existingJson?.videoUrl != videoUrl) {
            changes["videoUrl"] = CRDTEngine.nullableStringValue(videoUrl)
        }

        val newClock = existingClock.increment(deviceId)

        val updatedJson = LinkJson(
            url = url,
            domain = resolvedDomain,
            linkTypeCode = linkType.code,
            videoUrl = videoUrl,
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeLinkJson(bookmarkId, updatedJson)

        // 2. Record CRDT events
        if (changes.isNotEmpty()) {
            crdtEngine.recordFieldChanges(
                objectId = bookmarkId,
                objectType = ObjectType.BOOKMARK,
                file = FileTarget.LINK_JSON,
                changes = changes,
                currentClock = existingClock,
            )
        }

        // 3. Update SQLite cache
        linkBookmarkDao.upsert(updatedJson.toEntity(bookmarkId))
    }

    /**
     * Creates or updates link details based on whether they already exist.
     */
    suspend fun createOrUpdateLinkDetails(
        bookmarkId: Uuid,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        val existingJson = entityFileManager.readLinkJson(bookmarkId)
        if (existingJson == null) {
            createLinkDetails(bookmarkId, url, domain, linkType, videoUrl)
        } else {
            updateLinkDetails(bookmarkId, url, domain, linkType, videoUrl)
        }
    }

    // ==================== Private Helpers ====================

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }

    private suspend fun recordLinkCreationEvents(
        bookmarkId: Uuid,
        json: LinkJson,
    ) {
        val changes = mapOf(
            "url" to JsonPrimitive(json.url),
            "domain" to JsonPrimitive(json.domain),
            "linkTypeCode" to JsonPrimitive(json.linkTypeCode),
            "videoUrl" to CRDTEngine.nullableStringValue(json.videoUrl),
        )
        crdtEngine.recordFieldChanges(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.LINK_JSON,
            changes = changes,
            currentClock = VectorClock.empty(),
        )
    }

    // ==================== Mappers ====================

    private fun LinkJson.toEntity(bookmarkId: Uuid): LinkBookmarkEntity = LinkBookmarkEntity(
        bookmarkId = bookmarkId.toString(),
        url = url,
        domain = domain,
        linkType = LinkType.fromCode(linkTypeCode),
        videoUrl = videoUrl,
    )
}
