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
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock

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

    suspend fun getLinkmarkDetail(bookmarkId: String): LinkmarkUiModel? {
        val linkBookmark = bookmarkDao.getLinkBookmarkById(bookmarkId) ?: return null
        val domain = linkBookmark.toModel()
        val folder = folderDao.getFolderWithBookmarkCount(domain.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath =
            domain.localImagePath?.let { relativePath -> BookmarkFileManager.getAbsolutePath(relativePath) }
        val localIconAbsolutePath =
            domain.localIconPath?.let { relativePath -> BookmarkFileManager.getAbsolutePath(relativePath) }

        return domain.toUiModel(
            folder = folder,
            tags = tags,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
        )
    }

    // ==================== Write Operations (Enqueued) ====================

    /**
     * Enqueues link details creation.
     */
    fun createLinkDetails(
        bookmarkId: String,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        CoreOperationQueue.queue("CreateLinkDetails:$bookmarkId") {
            createLinkDetailsInternal(bookmarkId, url, domain, linkType, videoUrl)
        }
    }

    private suspend fun createLinkDetailsInternal(
        bookmarkId: String,
        url: String,
        domain: String?,
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
     * Enqueues link details update.
     */
    fun updateLinkDetails(
        bookmarkId: String,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        CoreOperationQueue.queue("UpdateLinkDetails:$bookmarkId") {
            updateLinkDetailsInternal(bookmarkId, url, domain, linkType, videoUrl)
        }
    }

    private suspend fun updateLinkDetailsInternal(
        bookmarkId: String,
        url: String,
        domain: String?,
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
     * Enqueues link details create or update.
     */
    fun createOrUpdateLinkDetails(
        bookmarkId: String,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateLinkDetails:$bookmarkId") {
            val existingJson = entityFileManager.readLinkJson(bookmarkId)
            if (existingJson == null) {
                createLinkDetailsInternal(bookmarkId, url, domain, linkType, videoUrl)
            } else {
                updateLinkDetailsInternal(bookmarkId, url, domain, linkType, videoUrl)
            }
        }
    }

    // ==================== Private Helpers ====================

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }

    private suspend fun recordLinkCreationEvents(
        bookmarkId: String,
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

    private fun LinkJson.toEntity(bookmarkId: String): LinkBookmarkEntity = LinkBookmarkEntity(
        bookmarkId = bookmarkId,
        url = url,
        domain = domain,
        linkType = LinkType.fromCode(linkTypeCode),
        videoUrl = videoUrl,
    )
}
