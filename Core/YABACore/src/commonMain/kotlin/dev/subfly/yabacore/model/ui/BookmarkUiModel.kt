package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.BookmarkKind
import kotlin.time.Instant

@Stable
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
