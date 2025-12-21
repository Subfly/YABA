@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.impex.model.CodableBookmark
import dev.subfly.yabacore.impex.model.ImportExportError
import dev.subfly.yabacore.impex.model.MappableCsvHeader
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object CsvImporter {
    fun import(
        bytes: ByteArray,
        existingIds: ExistingIds,
    ): ImportBundle {
        val content = bytes.decodeToString()
        val rows = CsvUtils.parse(content)
        if (rows.isEmpty()) throw ImportExportError.EmptyCsv

        val header = rows.first()
        if (header != CsvUtils.expectedHeader) throw ImportExportError.InvalidCsvHeader(header)
        if (rows.size <= 1) throw ImportExportError.EmptyCsv

        val now = Clock.System.now()
        val bookmarkIdResolver = IdResolver(existingIds.bookmarks)
        val fallbackFolder = createFallbackFolder(now, existingIds.nextRootOrder)

        val bookmarks = rows.drop(1).map { columns ->
            if (columns.size != CsvUtils.expectedHeader.size) {
                throw ImportExportError.InvalidCsv
            }
            val link = columns[3].ifBlank { throw ImportExportError.InvalidCsv }
            val bookmark = CodableBookmark(
                bookmarkId = columns[0].ifBlank { null },
                label = columns[1].ifBlank { null },
                description = columns[2].ifBlank { null },
                link = link,
                domain = columns[4].ifBlank { null },
                createdAt = columns[5].ifBlank { null },
                editedAt = columns[6].ifBlank { null },
                imageUrl = columns[7].ifBlank { null },
                iconUrl = columns[8].ifBlank { null },
                videoUrl = columns[9].ifBlank { null },
                readableHTML = null,
                type = columns[10].toIntOrNull(),
                version = columns[11].toIntOrNull(),
                imageData = null,
                iconData = null,
            )
            bookmark.toDomain(
                idResolver = bookmarkIdResolver,
                folderId = fallbackFolder.id,
                now = now,
            )
        }

        return ImportBundle(
            folders = listOf(fallbackFolder),
            tags = emptyList(),
            bookmarks = bookmarks,
            tagLinks = emptyList(),
        )
    }

    fun importMapped(
        bytes: ByteArray,
        headers: Map<MappableCsvHeader, Int?>,
        existingIds: ExistingIds,
    ): ImportBundle {
        val urlIndex = headers[MappableCsvHeader.URL]
            ?: throw ImportExportError.MissingRequiredField("url")

        val content = bytes.decodeToString()
        val rows = CsvUtils.parse(content)
        if (rows.isEmpty()) throw ImportExportError.EmptyCsv

        val now = Clock.System.now()
        val bookmarkIdResolver = IdResolver(existingIds.bookmarks)
        val fallbackFolder = createFallbackFolder(now, existingIds.nextRootOrder)

        val bookmarks = rows.drop(1).map { columns ->
            if (urlIndex >= columns.size) throw ImportExportError.InvalidCsv
            val url = columns[urlIndex].trim()
            if (!url.startsWith("http")) throw ImportExportError.InvalidBookmarkUrl(url)

            val label = headers[MappableCsvHeader.LABEL]
                ?.takeIf { it < columns.size }?.let {
                    columns[it]
                }
            val description = headers[MappableCsvHeader.DESCRIPTION]
                ?.takeIf { it < columns.size }
                ?.let { columns[it] }
            val createdAt = headers[MappableCsvHeader.CREATED_AT]
                ?.takeIf { it < columns.size }
                ?.let { columns[it] }

            val bookmark = CodableBookmark(
                bookmarkId = null,
                label = label,
                description = description,
                link = url,
                domain = null,
                createdAt = createdAt,
                editedAt = createdAt,
                imageUrl = null,
                iconUrl = null,
                videoUrl = null,
                readableHTML = null,
                type = null,
                version = 0,
                imageData = null,
                iconData = null,
            )
            bookmark.toDomain(
                idResolver = bookmarkIdResolver,
                folderId = fallbackFolder.id,
                now = now,
            )
        }

        return ImportBundle(
            folders = listOf(fallbackFolder),
            tags = emptyList(),
            bookmarks = bookmarks,
            tagLinks = emptyList(),
        )
    }

    private fun createFallbackFolder(now: Instant, order: Int): FolderDomainModel =
        FolderDomainModel(
            id = Uuid.random(),
            parentId = null,
            label = "Imported $now",
            description = null,
            icon = "folder-01",
            color = YabaColor.NONE,
            createdAt = now,
            editedAt = now,
            order = order,
        )
}
