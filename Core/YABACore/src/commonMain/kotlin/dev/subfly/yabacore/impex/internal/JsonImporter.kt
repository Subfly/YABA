@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.impex.model.CodableContent
import dev.subfly.yabacore.impex.model.ImportExportError
import dev.subfly.yabacore.impex.model.TagLink
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

internal object JsonImporter {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun decodeContent(bytes: ByteArray): CodableContent =
        runCatching {
            json.decodeFromString<CodableContent>(bytes.decodeToString())
        }.getOrElse { throw ImportExportError.InvalidJson }

    fun buildBundle(
        content: CodableContent,
        existingIds: ExistingIds,
    ): ImportBundle {
        val now = Clock.System.now()
        val folderIdResolver = IdResolver(existingIds.folders)
        val tagIdResolver = IdResolver(existingIds.tags)
        val bookmarkIdResolver = IdResolver(existingIds.bookmarks)

        val resolvedBookmarkIds = mutableMapOf<String, Uuid>()
        content.bookmarks.forEach { bookmark ->
            val key = bookmark.bookmarkId ?: bookmark.link
            val resolved = bookmarkIdResolver.resolve(bookmark.bookmarkId)
            resolvedBookmarkIds[key] = resolved
        }

        val collections = content.collections.orEmpty()
        if (collections.isEmpty()) {
            val fallbackFolder = createFallbackFolder(
                now = now,
                order = existingIds.nextRootOrder,
            )
            val bookmarks = content.bookmarks.map { bookmark ->
                bookmark.toDomain(
                    idResolver = bookmarkIdResolver,
                    folderId = fallbackFolder.id,
                    now = now,
                    resolvedId = resolvedBookmarkIds[bookmark.bookmarkId ?: bookmark.link],
                )
            }
            return ImportBundle(
                folders = listOf(fallbackFolder),
                tags = emptyList(),
                bookmarks = bookmarks,
                tagLinks = emptyList(),
            )
        }

        val folderCollections = collections.filter { it.type == 1 }
        val tagCollections = collections.filter { it.type == 2 }

        val folderDomains = folderCollections.mapIndexed { index, collection ->
            val base = collection.toFolderDomain(
                idResolver = folderIdResolver,
                now = now
            ).copy(order = collection.order.takeIf { it >= 0 } ?: index)
            if (base.parentId == null) {
                base.copy(order = existingIds.nextRootOrder + base.order)
            } else base
        }

        val tagDomains = tagCollections.mapIndexed { index, collection ->
            collection.toTagDomain(
                idResolver = tagIdResolver,
                now = now,
            ).copy(order = collection.order.takeIf { it >= 0 } ?: index)
        }

        val bookmarkFolderAssignments = mutableMapOf<String, Uuid>()
        folderCollections.forEach { collection ->
            val folderId = folderIdResolver.resolve(collection.collectionId)
            collection.bookmarks.forEach { bookmarkIdString ->
                if (resolvedBookmarkIds.containsKey(bookmarkIdString)
                    && !bookmarkFolderAssignments.containsKey(bookmarkIdString)
                ) {
                    bookmarkFolderAssignments[bookmarkIdString] = folderId
                }
            }
        }

        val tagLinks = tagCollections.flatMap { collection ->
            val tagId = tagIdResolver.resolve(collection.collectionId)
            collection.bookmarks.map { bookmarkId ->
                resolvedBookmarkIds[bookmarkId]?.let { resolvedBookmarkId ->
                    TagLink(
                        tagId = tagId,
                        bookmarkId = resolvedBookmarkId
                    )
                }
            }
        }.filterNotNull()

        val importedRootNextOrder = folderDomains
            .filter { it.parentId == null }
            .maxOfOrNull { it.order + 1 } ?: existingIds.nextRootOrder
        val fallbackOrder = maxOf(existingIds.nextRootOrder, importedRootNextOrder)
        val fallbackFolder = createFallbackFolder(now, fallbackOrder)
        var needsFallback = bookmarkFolderAssignments.isEmpty()
        val bookmarks = content.bookmarks.map { bookmark ->
            val folderId = bookmarkFolderAssignments[bookmark.bookmarkId ?: ""]
                ?: fallbackFolder.id.also { needsFallback = true }

            bookmark.toDomain(
                idResolver = bookmarkIdResolver,
                folderId = folderId,
                now = now,
                resolvedId = resolvedBookmarkIds[bookmark.bookmarkId ?: bookmark.link],
            )
        }

        val folders = if (needsFallback) folderDomains + fallbackFolder else folderDomains

        return ImportBundle(
            folders = folders,
            tags = tagDomains,
            bookmarks = bookmarks,
            tagLinks = tagLinks,
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

