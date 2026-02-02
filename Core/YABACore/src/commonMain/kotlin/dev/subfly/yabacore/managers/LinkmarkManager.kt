package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

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
    private val highlightDao get() = DatabaseProvider.highlightDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine

    // ==================== Query Operations ====================

    suspend fun getLinkmarkDetail(bookmarkId: String): LinkmarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        val linkMetaData = linkBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localIconAbsolutePath = bookmarkMetaData.localIconPath?.let {
            relativePath -> BookmarkFileManager.getAbsolutePath(relativePath)
        }

        return LinkmarkUiModel(
            id = bookmarkMetaData.id,
            folderId = bookmarkMetaData.folderId,
            kind = bookmarkMetaData.kind,
            label = bookmarkMetaData.label,
            description = bookmarkMetaData.description,
            createdAt = Instant.fromEpochMilliseconds(bookmarkMetaData.createdAt),
            editedAt = Instant.fromEpochMilliseconds(bookmarkMetaData.editedAt),
            viewCount = bookmarkMetaData.viewCount,
            isPrivate = bookmarkMetaData.isPrivate,
            isPinned = bookmarkMetaData.isPinned,
            url = linkMetaData.url,
            domain = linkMetaData.domain,
            linkType = linkMetaData.linkType,
            videoUrl = linkMetaData.videoUrl,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
            readableVersions = emptyList(),
        )
    }

    /**
     * Observes a linkmark and its highlights in real-time.
     *
     * Combines:
     * - Readable versions from readableVersionDao
     * - Assets from readableAssetDao
     * - Highlights from highlightDao
     *
     * Note: For full linkmark observation, use with bookmarkDao.observeById
     * and linkBookmarkDao.observeByBookmarkId if needed.
     *
     * @param bookmarkId The bookmark ID to observe
     * @return Flow emitting list of ReadableVersionUiModel with highlights
     */
    fun observeHighlights(bookmarkId: String): Flow<List<HighlightUiModel>> {
        return highlightDao.observeByBookmarkId(bookmarkId)
            .map { highlights ->
                highlights.map { entity -> entity.toUiModel() }
            }
    }

    // ==================== Write Operations (Enqueued) ====================

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

        // 2. Record CRDT CREATE event (for link.json file)
        crdtEngine.recordCreate(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.LINK_JSON,
            payload = buildLinkCreatePayload(linkJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        linkBookmarkDao.upsert(linkJson.toEntity(bookmarkId))
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
        val changes = mutableMapOf<String, JsonElement>()
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

        // 2. Record CRDT UPDATE event (only if there are changes)
        if (changes.isNotEmpty()) {
            crdtEngine.recordUpdate(
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

    private fun buildLinkCreatePayload(json: LinkJson): Map<String, JsonElement> =
        mapOf(
            "url" to JsonPrimitive(json.url),
            "domain" to JsonPrimitive(json.domain),
            "linkTypeCode" to JsonPrimitive(json.linkTypeCode),
            "videoUrl" to CRDTEngine.nullableStringValue(json.videoUrl),
        )

    // ==================== Mappers ====================

    private fun LinkJson.toEntity(bookmarkId: String): LinkBookmarkEntity = LinkBookmarkEntity(
        bookmarkId = bookmarkId,
        url = url,
        domain = domain,
        linkType = LinkType.fromCode(linkTypeCode),
        videoUrl = videoUrl,
    )

    private suspend fun HighlightEntity.toUiModel(): HighlightUiModel =
        HighlightUiModel(
            id = id,
            startSectionKey = startSectionKey,
            startOffsetInSection = startOffsetInSection,
            endSectionKey = endSectionKey,
            endOffsetInSection = endOffsetInSection,
            colorRole = colorRole,
            note = note,
            absolutePath = BookmarkFileManager.getAbsolutePath(relativePath),
            createdAt = createdAt,
            editedAt = editedAt,
        )
}
