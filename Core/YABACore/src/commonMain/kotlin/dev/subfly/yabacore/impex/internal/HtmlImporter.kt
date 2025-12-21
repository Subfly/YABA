@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.impex.model.CodableBookmark
import dev.subfly.yabacore.impex.model.ImportExportError
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object HtmlImporter {
    fun import(
        bytes: ByteArray,
        existingIds: ExistingIds,
    ): ImportBundle {
        val html = bytes.decodeToString()
        val parsed = HtmlParser.parse(html)
        if (parsed.folders.isEmpty() && parsed.rootBookmarks.isEmpty()) {
            throw ImportExportError.EmptyHtml
        }

        val now = Clock.System.now()
        val folderIdResolver = IdResolver(existingIds.folders)
        val bookmarkIdResolver = IdResolver(existingIds.bookmarks)

        val folders = mutableListOf<FolderDomainModel>()
        val bookmarks = mutableListOf<LinkBookmarkDomainModel>()
        var orderCounter = existingIds.nextRootOrder

        fun addBookmarks(
            nodes: List<HtmlParser.BookmarkNode>,
            folderId: Uuid,
        ) {
            nodes.forEach { node ->
                val bookmark = CodableBookmark(
                    bookmarkId = null,
                    label = node.title.ifBlank { node.url },
                    description = "",
                    link = node.url,
                    domain = null,
                    createdAt = now.toString(),
                    editedAt = now.toString(),
                    imageUrl = null,
                    iconUrl = null,
                    videoUrl = null,
                    readableHTML = null,
                    type = 1,
                    version = 1,
                    imageData = null,
                    iconData = null,
                ).toDomain(
                    idResolver = bookmarkIdResolver,
                    folderId = folderId,
                    now = now,
                )
                bookmarks += bookmark
            }
        }

        fun createFolderTree(
            node: HtmlParser.FolderNode,
            parentId: Uuid?,
        ) {
            val folderId = folderIdResolver.resolve(null)
            val folder = FolderDomainModel(
                id = folderId,
                parentId = parentId,
                label = node.label,
                description = null,
                icon = "folder-01",
                color = YabaColor.NONE,
                createdAt = now,
                editedAt = now,
                order = orderCounter++,
            )
            folders += folder
            addBookmarks(node.bookmarks, folderId)
            node.children.forEach { child ->
                createFolderTree(child, folderId)
            }
        }

        parsed.folders.forEach { createFolderTree(it, null) }

        if (parsed.rootBookmarks.isNotEmpty()) {
            val uncategorized = FolderDomainModel(
                id = folderIdResolver.resolve(null),
                parentId = null,
                label = "Imported $now",
                description = null,
                icon = "folder-01",
                color = YabaColor.NONE,
                createdAt = now,
                editedAt = now,
                order = orderCounter++,
            )
            folders += uncategorized
            addBookmarks(parsed.rootBookmarks, uncategorized.id)
        }

        return ImportBundle(
            folders = folders,
            tags = emptyList(),
            bookmarks = bookmarks,
            tagLinks = emptyList(),
        )
    }
}
