package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.BookmarkKind
import kotlin.time.Instant

@Stable
data class ImagemarkUiModel(
    override val id: String,
    override val folderId: String,
    override val kind: BookmarkKind = BookmarkKind.IMAGE,
    override val label: String,
    override val description: String?,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPrivate: Boolean = false,
    override val isPinned: Boolean = false,
    val summary: String? = null,
    /** Local file path for the bookmark's image. Imagemarks do not use localIconPath. */
    override val localImagePath: String? = null,
    override val localIconPath: String? = null,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
) : BookmarkUiModel
