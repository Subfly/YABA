package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.Instant

sealed interface BookmarkUiModel {
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
    /** Absolute file path for the bookmark's preview image, if available. */
    val localImagePath: String?
    /** Absolute file path for the bookmark's preview icon, if available. */
    val localIconPath: String?
    val parentFolder: FolderUiModel?
    val tags: List<TagUiModel>
}

/**
 * Base bookmark preview model used by list/grid views throughout the app.
 *
 * Subtype-specific details (e.g., link url/domain) are intentionally excluded and should be loaded
 * only when navigating to detail screens.
 */
@Immutable
data class BookmarkPreviewUiModel(
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
    /** Absolute file path for the bookmark's preview image, if available. */
    override val localImagePath: String? = null,
    /** Absolute file path for the bookmark's preview icon, if available. */
    override val localIconPath: String? = null,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
) : BookmarkUiModel

@Immutable
data class LinkmarkUiModel(
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
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val videoUrl: String?,
    /** Local file path for the bookmark's preview image, if saved to disk. */
    override val localImagePath: String? = null,
    /** Local file path for the bookmark's icon (domain icon for Linkmarks), if saved to disk. */
    override val localIconPath: String? = null,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
) : BookmarkUiModel
