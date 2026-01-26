package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.BookmarkKind
import kotlin.time.Instant

/**
 * Base bookmark preview model used by list/grid views throughout the app.
 *
 * Subtype-specific details (e.g., link url/domain) are intentionally excluded and should be loaded
 * only when navigating to detail screens.
 */
@Stable
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
