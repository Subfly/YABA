package dev.subfly.yabacore.markdown.formatting

import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.ast.DocumentNode
import dev.subfly.yabacore.markdown.ast.InlineNode
import dev.subfly.yabacore.markdown.core.SectionKey

/**
 * Builds [PreviewDocumentUiModel] from parsed AST.
 * Assigns deterministic sectionKey per block (b:<blockIndex>).
 * Resolves image paths via [assetPathByAssetId].
 */
object PreviewModelBuilder {

    fun build(
        document: DocumentNode,
        assetPathByAssetId: Map<String, String?> = emptyMap(),
    ): PreviewDocumentUiModel = build(document.children, assetPathByAssetId)

    fun build(
        blocks: List<BlockNode>,
        assetPathByAssetId: Map<String, String?> = emptyMap(),
    ): PreviewDocumentUiModel {
        val previewBlocks = blocks.mapIndexed { index, block ->
            toPreviewBlock(block, SectionKey.fromBlockIndex(index), assetPathByAssetId)
        }
        return PreviewDocumentUiModel(previewBlocks)
    }

    private fun toPreviewBlock(
        block: BlockNode,
        sectionKey: String,
        assetPathByAssetId: Map<String, String?>,
    ): PreviewBlockUiModel = when (block) {
        is BlockNode.Document -> throw IllegalArgumentException("Document should not be converted directly")
        is BlockNode.Heading -> PreviewBlockUiModel.Heading(
            sectionKey = sectionKey,
            range = block.range,
            level = block.level,
            inlineRuns = inlineToRuns(block.inline, assetPathByAssetId),
        )

        is BlockNode.Paragraph -> PreviewBlockUiModel.Paragraph(
            sectionKey = sectionKey,
            range = block.range,
            inlineRuns = inlineToRuns(block.inline, assetPathByAssetId),
        )

        is BlockNode.BlockQuote -> {
            val firstChild = block.children.firstOrNull() as? BlockNode.Paragraph
            val runs =
                firstChild?.let { inlineToRuns(it.inline, assetPathByAssetId) } ?: emptyList()
            PreviewBlockUiModel.BlockQuote(
                sectionKey = sectionKey,
                range = block.range,
                inlineRuns = runs,
            )
        }

        is BlockNode.ListBlock -> PreviewBlockUiModel.ListBlock(
            sectionKey = sectionKey,
            range = block.range,
            ordered = block.ordered,
            items = block.children.map { item ->
                when (item) {
                    is BlockNode.ListItem -> inlineToRuns(item.inline, assetPathByAssetId)
                    else -> emptyList()
                }
            },
        )

        is BlockNode.ListItem -> throw IllegalArgumentException("ListItem should be under ListBlock")
        is BlockNode.CodeFence -> PreviewBlockUiModel.CodeFence(
            sectionKey = sectionKey,
            range = block.range,
            language = block.language,
            text = block.literal,
        )

        is BlockNode.HorizontalRule -> PreviewBlockUiModel.HorizontalRule(
            sectionKey = sectionKey,
            range = block.range,
        )

        is BlockNode.TableBlock -> PreviewBlockUiModel.TableBlock(
            sectionKey = sectionKey,
            range = block.range,
            header = block.header.map { inlineToRuns(it, assetPathByAssetId) },
            rows = block.rows.map { row ->
                row.map { cell ->
                    inlineToRuns(
                        cell,
                        assetPathByAssetId
                    )
                }
            },
        )
    }

    private fun inlineToRuns(
        nodes: List<InlineNode>,
        assetPathByAssetId: Map<String, String?> = emptyMap(),
    ): List<InlineRunUiModel> {
        val runs = mutableListOf<InlineRunUiModel>()
        for (node in nodes) {
            flattenInline(
                node,
                bold = false,
                italic = false,
                strikethrough = false,
                code = false,
                linkUrl = null,
                assetPathByAssetId,
                runs
            )
        }
        return runs
    }

    private fun flattenInline(
        node: InlineNode,
        bold: Boolean,
        italic: Boolean,
        strikethrough: Boolean,
        code: Boolean,
        linkUrl: String?,
        assetPathByAssetId: Map<String, String?>,
        out: MutableList<InlineRunUiModel>,
    ) {
        when (node) {
            is InlineNode.Text -> out.add(
                InlineRunUiModel.Text(
                    range = node.range,
                    text = node.literal,
                    bold = bold,
                    italic = italic,
                    strikethrough = strikethrough,
                    code = code,
                    linkUrl = linkUrl,
                )
            )

            is InlineNode.Emphasis -> node.children.forEach {
                flattenInline(
                    it,
                    bold,
                    true,
                    strikethrough,
                    code,
                    linkUrl,
                    assetPathByAssetId,
                    out
                )
            }

            is InlineNode.Strong -> node.children.forEach {
                flattenInline(
                    it,
                    true,
                    italic,
                    strikethrough,
                    code,
                    linkUrl,
                    assetPathByAssetId,
                    out
                )
            }

            is InlineNode.Strikethrough -> node.children.forEach {
                flattenInline(
                    it,
                    bold,
                    italic,
                    true,
                    code,
                    linkUrl,
                    assetPathByAssetId,
                    out
                )
            }

            is InlineNode.InlineCode -> out.add(
                InlineRunUiModel.Text(
                    range = node.range,
                    text = node.literal,
                    bold = bold,
                    italic = italic,
                    strikethrough = strikethrough,
                    code = true,
                    linkUrl = linkUrl,
                )
            )

            is InlineNode.Link -> node.children.forEach {
                flattenInline(
                    it,
                    bold,
                    italic,
                    strikethrough,
                    code,
                    node.url,
                    assetPathByAssetId,
                    out
                )
            }

            is InlineNode.Image -> {
                val assetId = node.url.substringAfterLast("/").substringBeforeLast(".")
                val path = assetPathByAssetId[assetId] ?: node.url
                out.add(
                    InlineRunUiModel.Image(
                        range = node.range,
                        assetId = assetId,
                        path = path,
                        alt = node.alt,
                        caption = node.title,
                    )
                )
            }

            is InlineNode.SoftBreak -> out.add(
                InlineRunUiModel.Text(
                    range = node.range,
                    text = "\n",
                    bold = bold,
                    italic = italic,
                    strikethrough = strikethrough,
                    code = code,
                    linkUrl = linkUrl
                )
            )

            is InlineNode.HardBreak -> out.add(
                InlineRunUiModel.Text(
                    range = node.range,
                    text = "\n",
                    bold = bold,
                    italic = italic,
                    strikethrough = strikethrough,
                    code = code,
                    linkUrl = linkUrl
                )
            )
        }
    }
}
