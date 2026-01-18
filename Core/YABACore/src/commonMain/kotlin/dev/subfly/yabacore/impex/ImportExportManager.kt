package dev.subfly.yabacore.impex

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.impex.internal.CsvImporter
import dev.subfly.yabacore.impex.internal.CsvUtils
import dev.subfly.yabacore.impex.internal.ExistingIds
import dev.subfly.yabacore.impex.internal.Exporters
import dev.subfly.yabacore.impex.internal.HtmlImporter
import dev.subfly.yabacore.impex.internal.ImportBundle
import dev.subfly.yabacore.impex.internal.ImportExportDataSource
import dev.subfly.yabacore.impex.internal.JsonImporter
import dev.subfly.yabacore.impex.model.ExportFormat
import dev.subfly.yabacore.impex.model.ImportSummary
import dev.subfly.yabacore.impex.model.MappableCsvHeader
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Facade for import/export operations implemented in the shared Kotlin core.
 *
 * Uses filesystem-first approach for imports - data is written to JSON files
 * first, then SQLite cache is updated.
 */
object ImportExportManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val entityFileManager get() = EntityFileManager

    suspend fun importJson(bytes: ByteArray): ImportSummary =
        import { existing ->
            JsonImporter.buildBundle(
                JsonImporter.decodeContent(bytes),
                existing
            )
        }

    suspend fun importCsv(bytes: ByteArray): ImportSummary =
        import { existing -> CsvImporter.import(bytes, existing) }

    suspend fun importMappedCsv(
        bytes: ByteArray,
        headers: Map<MappableCsvHeader, Int?>,
    ): ImportSummary = import { existing ->
        CsvImporter.importMapped(bytes, headers, existing)
    }

    suspend fun importHtml(bytes: ByteArray): ImportSummary =
        import { existing -> HtmlImporter.import(bytes, existing) }

    suspend fun export(format: ExportFormat): ByteArray =
        withContext(Dispatchers.Default) {
            val snapshot = ImportExportDataSource.loadSnapshot()
            Exporters.export(snapshot, format)
        }

    fun extractCsvHeaders(bytes: ByteArray): List<String> {
        val content = bytes.decodeToString()
        val firstLine = content
            .lineSequence()
            .firstOrNull { it.isNotBlank() } ?: return emptyList()
        return CsvUtils.parseRow(firstLine)
    }

    private suspend fun import(
        builder: suspend (existing: ExistingIds) -> ImportBundle,
    ): ImportSummary = withContext(Dispatchers.Default) {
        val existing = ImportExportDataSource.existingIds()
        val bundle = builder(existing)
        applyImportBundle(bundle)
        ImportSummary(
            folders = bundle.folders.size,
            tags = bundle.tags.size,
            bookmarks = bundle.bookmarks.size,
        )
    }

    /**
     * Applies an ImportBundle using the filesystem-first approach.
     */
    private suspend fun applyImportBundle(bundle: ImportBundle) {
        val deviceId = DeviceIdProvider.get()

        // Import folders
        bundle.folders.sortedBy { it.order }.forEach { folder ->
            val initialClock = VectorClock.of(deviceId, 1)
            val folderJson = FolderMetaJson(
                id = folder.id,
                parentId = folder.parentId,
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                colorCode = folder.color.code,
                order = folder.order,
                createdAt = folder.createdAt.toEpochMilliseconds(),
                editedAt = folder.editedAt.toEpochMilliseconds(),
                clock = initialClock.toMap(),
                isHidden = false,
            )

            // 1. Write to filesystem
            entityFileManager.writeFolderMeta(folderJson)

            // 2. Update SQLite cache
            folderDao.upsert(
                FolderEntity(
                    id = folder.id,
                    parentId = folder.parentId,
                    label = folder.label,
                    description = folder.description,
                    icon = folder.icon,
                    color = folder.color,
                    order = folder.order,
                    createdAt = folder.createdAt.toEpochMilliseconds(),
                    editedAt = folder.editedAt.toEpochMilliseconds(),
                    isHidden = false,
                )
            )
        }

        // Import tags
        bundle.tags.sortedBy { it.order }.forEach { tag ->
            val initialClock = VectorClock.of(deviceId, 1)
            val tagJson = TagMetaJson(
                id = tag.id,
                label = tag.label,
                icon = tag.icon,
                colorCode = tag.color.code,
                order = tag.order,
                createdAt = tag.createdAt.toEpochMilliseconds(),
                editedAt = tag.editedAt.toEpochMilliseconds(),
                clock = initialClock.toMap(),
                isHidden = false,
            )

            // 1. Write to filesystem
            entityFileManager.writeTagMeta(tagJson)

            // 2. Update SQLite cache
            tagDao.upsert(
                TagEntity(
                    id = tag.id,
                    label = tag.label,
                    icon = tag.icon,
                    color = tag.color,
                    order = tag.order,
                    createdAt = tag.createdAt.toEpochMilliseconds(),
                    editedAt = tag.editedAt.toEpochMilliseconds(),
                    isHidden = false,
                )
            )
        }

        // Import bookmarks
        bundle.bookmarks.forEach { linkBookmark ->
            val initialClock = VectorClock.of(deviceId, 1)

            val bookmarkJson = BookmarkMetaJson(
                id = linkBookmark.id,
                folderId = linkBookmark.folderId,
                kind = linkBookmark.kind.code,
                label = linkBookmark.label,
                description = linkBookmark.description,
                createdAt = linkBookmark.createdAt.toEpochMilliseconds(),
                editedAt = linkBookmark.editedAt.toEpochMilliseconds(),
                viewCount = linkBookmark.viewCount,
                isPrivate = linkBookmark.isPrivate,
                isPinned = linkBookmark.isPinned,
                localImagePath = linkBookmark.localImagePath,
                localIconPath = linkBookmark.localIconPath,
                tagIds = emptyList(), // Will be linked separately
                clock = initialClock.toMap(),
            )

            // 1. Write bookmark meta.json to filesystem
            entityFileManager.writeBookmarkMeta(bookmarkJson)

            // Create link.json
            val linkJson = LinkJson(
                url = linkBookmark.url,
                domain = linkBookmark.domain,
                linkTypeCode = linkBookmark.linkType.code,
                videoUrl = linkBookmark.videoUrl,
                clock = initialClock.toMap(),
            )

            // 2. Write link.json to filesystem
            entityFileManager.writeLinkJson(linkBookmark.id, linkJson)

            // 3. Update SQLite cache
            bookmarkDao.upsert(
                BookmarkEntity(
                    id = linkBookmark.id,
                    folderId = linkBookmark.folderId,
                    kind = linkBookmark.kind,
                    label = linkBookmark.label,
                    description = linkBookmark.description,
                    createdAt = linkBookmark.createdAt.toEpochMilliseconds(),
                    editedAt = linkBookmark.editedAt.toEpochMilliseconds(),
                    viewCount = linkBookmark.viewCount,
                    isPrivate = linkBookmark.isPrivate,
                    isPinned = linkBookmark.isPinned,
                    localImagePath = linkBookmark.localImagePath,
                    localIconPath = linkBookmark.localIconPath,
                )
            )

            linkBookmarkDao.upsert(
                LinkBookmarkEntity(
                    bookmarkId = linkBookmark.id,
                    url = linkBookmark.url,
                    domain = linkBookmark.domain,
                    linkType = linkBookmark.linkType,
                    videoUrl = linkBookmark.videoUrl,
                )
            )
        }

        // Import tag links
        bundle.tagLinks.forEach { link ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = link.tagId,
                    bookmarkId = link.bookmarkId,
                )
            )
        }
    }
}
