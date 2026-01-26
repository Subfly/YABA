package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
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
    /** Readable versions ordered by contentVersion DESC (newest first) for "Time Machine" */
    val readableVersions: List<ReadableVersionUiModel> = emptyList(),
) : BookmarkUiModel {
    /** The latest readable version, or null if no versions exist */
    val latestVersion: ReadableVersionUiModel?
        get() = readableVersions.firstOrNull()

    /** Total number of readable versions */
    val versionCount: Int
        get() = readableVersions.size

    /** Total number of highlights across all versions */
    val totalHighlightCount: Int
        get() = readableVersions.sumOf { it.highlights.size }
}
