package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.BookmarkKind
import kotlin.time.Instant

@Stable
data class NotemarkUiModel(
    override val id: String,
    override val folderId: String,
    override val kind: BookmarkKind = BookmarkKind.NOTE,
    override val label: String,
    override val description: String?,
    override val createdAt: Instant,
    override val editedAt: Instant,
    override val viewCount: Long = 0,
    override val isPrivate: Boolean = false,
    override val isPinned: Boolean = false,
    /** Relative path to canonical note body markdown (e.g. `bookmarks/<id>/note/body.md`). */
    val markdownRelativePath: String,
    /** Readable version id used for highlight anchors. */
    val readableVersionId: String,
    override val localImagePath: String? = null,
    override val localIconPath: String? = null,
    override val parentFolder: FolderUiModel?,
    override val tags: List<TagUiModel> = emptyList(),
) : BookmarkUiModel
