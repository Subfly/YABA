package dev.subfly.yabacore.state.detail.linkmark

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.LinkType

@Immutable
data class LinkmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val linkDetails: LinkmarkLinkDetailsUiModel? = null,
    val readableVersions: List<ReadableVersionUiModel> = emptyList(),
    val highlights: List<HighlightUiModel> = emptyList(),
    val isLoading: Boolean = false,
)

@Immutable
data class LinkmarkLinkDetailsUiModel(
    val url: String,
    val domain: String,
    val linkType: LinkType,
    val videoUrl: String?,
)
