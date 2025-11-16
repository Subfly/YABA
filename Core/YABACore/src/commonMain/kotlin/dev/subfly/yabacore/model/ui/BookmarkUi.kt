@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.model.ui

import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.ExperimentalTime
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
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val previewImageUrl: String?,
    val previewIconUrl: String?,
    val videoUrl: String?,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
) : BookmarkUiModel

