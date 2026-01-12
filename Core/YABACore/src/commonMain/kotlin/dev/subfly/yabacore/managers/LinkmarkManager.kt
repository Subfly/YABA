@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object LinkmarkManager {
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao
    private val opApplier
        get() = OpApplier
    private val linkmarkFileManager
        get() = LinkmarkFileManager
    private val clock = Clock.System

    suspend fun createLinkmark(linkmark: LinkmarkUiModel): LinkmarkUiModel {
        val now = clock.now()
        val domain = LinkBookmarkDomainModel(
            id = linkmark.id,
            folderId = linkmark.folderId,
            kind = BookmarkKind.LINK,
            label = linkmark.label.takeIf { it.isNotBlank() } ?: linkmark.url,
            description = linkmark.description,
            createdAt = now,
            editedAt = now,
            viewCount = linkmark.viewCount,
            isPrivate = linkmark.isPrivate,
            isPinned = linkmark.isPinned,
            localImagePath = null,
            localIconPath = null,
            url = linkmark.url,
            domain = extractDomain(linkmark.url),
            linkType = linkmark.linkType,
            videoUrl = linkmark.videoUrl,
        )
        opApplier.applyLocal(listOf(domain.toOperationDraft(OperationKind.CREATE)))
        return domain.toUiModel()
    }

    suspend fun updateLinkmark(linkmark: LinkmarkUiModel): LinkmarkUiModel? {
        val existing = bookmarkDao.getLinkBookmarkById(linkmark.id.toString())?.toModel() ?: return null
        val now = clock.now()
        val updated = existing.copy(
            folderId = linkmark.folderId,
            label = linkmark.label.takeIf { it.isNotBlank() } ?: existing.label,
            description = linkmark.description,
            url = linkmark.url,
            domain = extractDomain(linkmark.url),
            linkType = linkmark.linkType,
            videoUrl = linkmark.videoUrl,
            editedAt = now,
            viewCount = linkmark.viewCount,
            isPrivate = linkmark.isPrivate,
            isPinned = linkmark.isPinned,
        )
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
        return updated.toUiModel()
    }

    suspend fun getLinkmarkDetail(bookmarkId: Uuid): LinkmarkUiModel? {
        val linkBookmark = bookmarkDao.getLinkBookmarkById(bookmarkId.toString())?.toModel() ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(linkBookmark.folderId.toString())?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId.toString()).map { it.toUiModel() }
        return linkBookmark.toUiModel(folder = folder, tags = tags)
    }

    suspend fun saveLinkImage(
        bookmarkId: Uuid,
        bytes: ByteArray,
        extension: String = "jpeg",
    ): PlatformFile = linkmarkFileManager.saveLinkImageBytes(bookmarkId, bytes, extension)

    suspend fun importLinkImageFromFile(
        bookmarkId: Uuid,
        source: PlatformFile,
    ): PlatformFile = linkmarkFileManager.importLinkImageFromFile(bookmarkId, source)

    suspend fun getLinkImageFile(bookmarkId: Uuid): PlatformFile? =
        linkmarkFileManager.getLinkImageFile(bookmarkId)

    suspend fun saveDomainIcon(
        bookmarkId: Uuid,
        bytes: ByteArray,
    ): PlatformFile = linkmarkFileManager.saveDomainIconBytes(bookmarkId, bytes)

    suspend fun importDomainIconFromFile(
        bookmarkId: Uuid,
        source: PlatformFile,
    ): PlatformFile = linkmarkFileManager.importDomainIconFromFile(bookmarkId, source)

    suspend fun getDomainIconFile(bookmarkId: Uuid): PlatformFile? =
        linkmarkFileManager.getDomainIconFile(bookmarkId)

    suspend fun clearLinkmarkFiles(bookmarkId: Uuid) {
        linkmarkFileManager.purgeLinkmarkFolder(bookmarkId)
    }

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }
}
