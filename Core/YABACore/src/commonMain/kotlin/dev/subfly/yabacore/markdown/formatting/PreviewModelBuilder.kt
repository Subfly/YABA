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
            val paragraphRuns = block.children
                .filterIsInstance<BlockNode.Paragraph>()
                .map { inlineToRuns(it.inline, assetPathByAssetId) }
            val runs = paragraphRuns.flatMapIndexed { index, runs ->
                if (index > 0) {
                    listOf(
                        InlineRunUiModel.Text(
                            range = block.range,
                            text = "\n\n",
                            bold = false,
                            italic = false,
                            strikethrough = false,
                            code = false,
                            linkUrl = null,
                        )
                    ) + runs
                } else runs
            }
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
                    is BlockNode.ListItem -> PreviewBlockUiModel.ListItem(
                        runs = inlineToRuns(item.inline, assetPathByAssetId),
                        checked = item.checked,
                    )
                    else -> PreviewBlockUiModel.ListItem(runs = emptyList(), checked = null)
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
            alignments = block.alignments,
        )

        is BlockNode.DefinitionList -> PreviewBlockUiModel.DefinitionList(
            sectionKey = sectionKey,
            range = block.range,
            items = block.children.map { item ->
                when (item) {
                    is BlockNode.DefinitionItem -> PreviewBlockUiModel.DefinitionItem(
                        term = inlineToRuns(item.term, assetPathByAssetId),
                        definitions = item.definitions.map { def -> inlineToRuns(def, assetPathByAssetId) },
                    )
                    else -> PreviewBlockUiModel.DefinitionItem(term = emptyList(), definitions = emptyList())
                }
            },
        )

        is BlockNode.DefinitionItem -> throw IllegalArgumentException("DefinitionItem should be under DefinitionList")
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
                superscript = false,
                subscript = false,
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
        superscript: Boolean,
        subscript: Boolean,
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
                    superscript = superscript,
                    subscript = subscript,
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
                    superscript,
                    subscript,
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
                    superscript,
                    subscript,
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
                    superscript,
                    subscript,
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
                    superscript = superscript,
                    subscript = subscript,
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
                    superscript,
                    subscript,
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
                    linkUrl = linkUrl,
                    superscript = superscript,
                    subscript = subscript,
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
                    linkUrl = linkUrl,
                    superscript = superscript,
                    subscript = subscript,
                )
            )

            is InlineNode.FootnoteRef -> out.add(
                InlineRunUiModel.Text(
                    range = node.range,
                    text = "[^${node.footnoteId}]",
                    bold = bold,
                    italic = italic,
                    strikethrough = strikethrough,
                    code = code,
                    linkUrl = linkUrl,
                    superscript = superscript,
                    subscript = subscript,
                )
            )

            is InlineNode.Superscript -> node.children.forEach {
                flattenInline(
                    it,
                    bold,
                    italic,
                    strikethrough,
                    code,
                    linkUrl,
                    true,
                    subscript,
                    assetPathByAssetId,
                    out
                )
            }

            is InlineNode.Subscript -> node.children.forEach {
                flattenInline(
                    it,
                    bold,
                    italic,
                    strikethrough,
                    code,
                    linkUrl,
                    superscript,
                    true,
                    assetPathByAssetId,
                    out
                )
            }
        }
    }
}
