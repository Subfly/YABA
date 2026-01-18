package dev.subfly.yabacore.impex.internal

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.impex.model.TagLink
import dev.subfly.yabacore.model.utils.BookmarkKind

internal data class ExistingIds(
    val folders: Set<String>,
    val tags: Set<String>,
    val bookmarks: Set<String>,
    val nextRootOrder: Int,
)

internal data class ExportSnapshot(
    val folders: List<FolderDomainModel>,
    val tags: List<TagDomainModel>,
    val bookmarks: List<LinkBookmarkDomainModel>,
    val tagLinks: List<TagLink>,
)

internal object ImportExportDataSource {
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao
        get() = DatabaseProvider.linkBookmarkDao

    suspend fun existingIds(): ExistingIds {
        val allFolders = folderDao.getAll()
        val folderIds = allFolders.map { it.id }.toSet()
        val tagIds = tagDao.getAll().map { it.id }.toSet()
        val bookmarkIds = bookmarkDao.getAll().map { it.id }.toSet()
        val rootMaxOrder = allFolders
            .filter { it.parentId == null }
            .maxOfOrNull { it.order } ?: -1
        return ExistingIds(
            folders = folderIds,
            tags = tagIds,
            bookmarks = bookmarkIds,
            nextRootOrder = rootMaxOrder + 1,
        )
    }

    suspend fun loadSnapshot(): ExportSnapshot {
        val folderDomains = folderDao.getAll().map { it.toModel() }
        val tagDomains = tagDao.getAll().map { it.toModel() }

        val bookmarkEntities = bookmarkDao.getAll().filter { it.kind == BookmarkKind.LINK }

        val bookmarks =
            bookmarkEntities.mapNotNull { bookmark ->
                val link = linkBookmarkDao.getByBookmarkId(bookmark.id)
                    ?: return@mapNotNull null
                bookmark.toModel(link)
            }

        val tagLinks =
            bookmarks.flatMap { bookmark ->
                tagDao.getTagsForBookmarkWithCounts(bookmark.id).map {
                    TagLink(tagId = it.tag.toModel().id, bookmarkId = bookmark.id)
                }
            }

        return ExportSnapshot(
            folders = folderDomains,
            tags = tagDomains,
            bookmarks = bookmarks,
            tagLinks = tagLinks,
        )
    }
}
