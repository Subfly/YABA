package dev.subfly.yabacore.database.domain

import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.Instant

internal sealed interface BookmarkDomainModel {
    val id: String
    val folderId: String
    val kind: BookmarkKind
    val label: String
    val description: String?
    val createdAt: Instant
    val editedAt: Instant
    val viewCount: Long
    val isPrivate: Boolean
    val isPinned: Boolean
    /**
     * Relative path (within the app-managed bookmarks directory) to a preview image, if available.
     * This should be resolved to an absolute path at the UI boundary.
     */
    val localImagePath: String?
    /**
     * Relative path (within the app-managed bookmarks directory) to a preview icon, if available.
     * This should be resolved to an absolute path at the UI boundary.
     */
    val localIconPath: String?
}

/**
 * Base bookmark metadata used for list/grid previews and general app flows.
 *
 * This intentionally does NOT include subtype-specific detail fields (e.g., Link url/domain).
 * Subtype details are saved and loaded separately (e.g., via LINK_BOOKMARK operations).
 */
internal data class BookmarkMetadataDomainModel(
    override val id: String,
    override val folderId: String,
    override val kind: BookmarkKind,
    override val label: String,
    override val description: String?,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPrivate: Boolean = false,
    override val isPinned: Boolean = false,
    override val localImagePath: String? = null,
    override val localIconPath: String? = null,
) : BookmarkDomainModel

internal data class LinkBookmarkDomainModel(
    override val id: String,
    override val folderId: String,
    override val kind: BookmarkKind = BookmarkKind.LINK,
    override val label: String,
    override val description: String?,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPrivate: Boolean = false,
    override val isPinned: Boolean = false,
    override val localImagePath: String? = null,
    override val localIconPath: String? = null,
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val videoUrl: String?,
) : BookmarkDomainModel
