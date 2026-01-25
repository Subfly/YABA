package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.model.utils.YabaColor
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

/**
 * A single readable content version with its assets and highlights.
 */
@Immutable
data class ReadableVersionUiModel(
    val contentVersion: Int,
    val createdAt: Long,
    val title: String?,
    val author: String?,
    val document: ReadableDocumentUiModel?,
    val assets: List<ReadableAssetUiModel> = emptyList(),
    val highlights: List<HighlightUiModel> = emptyList(),
)

/**
 * A readable content asset (image) with absolute path.
 */
@Immutable
data class ReadableAssetUiModel(
    val assetId: String,
    val role: ReadableAssetRole,
    /** Absolute path to the asset file */
    val absolutePath: String?,
)

/**
 * A highlight annotation.
 */
@Immutable
data class HighlightUiModel(
    val id: String,
    val startBlockId: String,
    val startInlinePath: List<Int>,
    val startOffset: Int,
    val endBlockId: String,
    val endInlinePath: List<Int>,
    val endOffset: Int,
    val colorRole: YabaColor,
    val note: String?,
    /** Absolute path to the highlight JSON file */
    val absolutePath: String?,
    val createdAt: Long,
    val editedAt: Long,
)
