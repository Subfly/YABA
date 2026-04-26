package dev.subfly.yaba.core.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yaba.core.model.utils.BookmarkKind
import kotlin.time.Instant

@Stable
data class LinkmarkUiModel(
    override val id: String,
    override val folderId: String,
    override val kind: BookmarkKind = BookmarkKind.LINK,
    override val label: String,
    override val description: String?,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPinned: Boolean = false,
    val url: String,
    val domain: String,
    val videoUrl: String?,
    val audioUrl: String? = null,
    val metadataTitle: String? = null,
    val metadataDescription: String? = null,
    val metadataAuthor: String? = null,
    val metadataDate: String? = null,
    /** Local file path for the bookmark's preview image, if saved to disk. */
    override val localImagePath: String? = null,
    /** Local file path for the bookmark's icon (domain icon for Linkmarks), if saved to disk. */
    override val localIconPath: String? = null,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
    /** Relative path to the current readable document JSON, if any. */
    val readableBodyRelativePath: String? = null,
    /** Asset paths (relative) for the current readable. */
    val readableAssetRelativePaths: List<String> = emptyList(),
) : BookmarkUiModel
