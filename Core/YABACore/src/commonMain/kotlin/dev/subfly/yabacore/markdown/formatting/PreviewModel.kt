package dev.subfly.yabacore.markdown.formatting

import dev.subfly.yabacore.markdown.core.Range

/**
 * UI-facing preview model: block list with deterministic sectionKey and inline runs (with ranges).
 * Used by MarkdownPreviewView for LazyColumn rendering and highlight overlay.
 */
data class PreviewDocumentUiModel(
    val blocks: List<PreviewBlockUiModel>,
)

/**
 * One block in the preview. sectionKey is deterministic (e.g. "b:<blockIndex>") for highlight anchors.
 */
sealed interface PreviewBlockUiModel {
    val sectionKey: String
    val range: Range

    data class Paragraph(
        override val sectionKey: String,
        override val range: Range,
        val inlineRuns: List<InlineRunUiModel>,
    ) : PreviewBlockUiModel

    data class Heading(
        override val sectionKey: String,
        override val range: Range,
        val level: Int,
        val inlineRuns: List<InlineRunUiModel>,
    ) : PreviewBlockUiModel

    data class BlockQuote(
        override val sectionKey: String,
        override val range: Range,
        val inlineRuns: List<InlineRunUiModel>,
    ) : PreviewBlockUiModel

    data class CodeFence(
        override val sectionKey: String,
        override val range: Range,
        val language: String?,
        val text: String,
    ) : PreviewBlockUiModel

    data class ListBlock(
        override val sectionKey: String,
        override val range: Range,
        val ordered: Boolean,
        val items: List<List<InlineRunUiModel>>,
    ) : PreviewBlockUiModel

    data class HorizontalRule(
        override val sectionKey: String,
        override val range: Range,
    ) : PreviewBlockUiModel

    data class TableBlock(
        override val sectionKey: String,
        override val range: Range,
        val header: List<List<InlineRunUiModel>>,
        val rows: List<List<List<InlineRunUiModel>>>,
    ) : PreviewBlockUiModel

    data class Image(
        override val sectionKey: String,
        override val range: Range,
        val assetId: String,
        val path: String?,
        val alt: String?,
        val caption: String?,
    ) : PreviewBlockUiModel
}

/**
 * One run within a block: either styled text or an image. range is absolute document range.
 */
sealed interface InlineRunUiModel {
    val range: Range

    data class Text(
        override val range: Range,
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false,
        val code: Boolean = false,
        val linkUrl: String? = null,
    ) : InlineRunUiModel

    data class Image(
        override val range: Range,
        val assetId: String,
        val path: String?,
        val alt: String?,
        val caption: String?,
    ) : InlineRunUiModel
}
