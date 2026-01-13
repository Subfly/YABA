@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.linkBookmarkOperationDraft
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.LinkType
import io.github.vinceglb.filekit.path
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

    /**
     * Saves/updates link-specific details (url/domain/linkType/videoUrl) for an existing bookmark.
     *
     * Bookmark metadata (folder/title/description/tags/preview assets) must be saved via
     * [AllBookmarksManager].
     */
    suspend fun createOrUpdateLinkDetails(
        bookmarkId: Uuid,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
        operationKind: OperationKind,
    ) {
        val now = clock.now()
        val resolvedDomain = domain?.takeIf { it.isNotBlank() } ?: extractDomain(url)
        opApplier.applyLocal(
            listOf(
                linkBookmarkOperationDraft(
                    bookmarkId = bookmarkId,
                    url = url,
                    domain = resolvedDomain,
                    linkType = linkType,
                    videoUrl = videoUrl,
                    kind = operationKind,
                    happenedAt = now,
                )
            )
        )
    }

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

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }
}
