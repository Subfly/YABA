@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.model.ui

import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface BookmarkUiModel {
    val id: Uuid
    val folderId: Uuid
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

data class LinkmarkUiModel(
    override val id: Uuid,
    override val folderId: Uuid,
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
