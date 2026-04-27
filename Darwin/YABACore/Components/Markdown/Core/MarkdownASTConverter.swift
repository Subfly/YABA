//
//  MarkdownASTConverter.swift
//  YABACore
//
//  Maps KMP `Document` trees into `MarkdownRenderDocument` for native preview/editor.
//

import Foundation
import MarkdownParser

public enum MarkdownASTConverter {
    public static func parse(markdown: String) -> MarkdownRenderDocument {
        let document = MarkdownParser.Companion()
            .parseToDocument(input: markdown, flavour: ExtendedFlavour())
        return convertDocument(document, source: markdown)
    }

    // MARK: - Document

    private static func convertDocument(_ document: Document, source _: String) -> MarkdownRenderDocument {
        var ctx = ConversionContext()
        ctx.harvestTocContext(from: document)
        let children = (document as ContainerNode).children
        var out: [MarkdownRenderBlock] = []
        for (i, n) in children.enumerated() {
            if let b = convertBlock(n, index: i, &ctx) {
                out.append(b)
            }
        }
        return MarkdownRenderDocument(blocks: out)
    }

    // MARK: - TOC (headings)

    private struct ConversionContext {
        var catalog: [TocHeading] = []
        mutating func harvestTocContext(from node: Node) { walkForHeadings(node) }
        private mutating func walkForHeadings(_ node: Node) {
            if let h = node as? Heading {
                let title = MarkdownInlinePlainText.plainText(from: InlineAssembler.build(from: h))
                catalog.append(TocHeading(headingId: h.id, level: Int(h.level), title: title))
            } else if let h = node as? SetextHeading {
                let title = MarkdownInlinePlainText.plainText(from: InlineAssembler.build(from: h))
                catalog.append(TocHeading(headingId: h.id, level: Int(h.level), title: title))
            }
            guard let c = node as? ContainerNode else { return }
            for ch in c.children { walkForHeadings(ch) }
        }
    }

    private struct TocHeading {
        let headingId: String?
        let level: Int
        let title: String
    }

    // MARK: - Block

    private static func makeId(_ node: Node, index: Int) -> String {
        "\(node.stableKey)-\(index)-\(node.contentHash)"
    }

    private static func convertBlock(_ node: Node, index: Int, _ ctx: inout ConversionContext) -> MarkdownRenderBlock? {
        let id = makeId(node, index: index)
        if node is BlankLine { return nil }

        if node is ThematicBreak {
            return MarkdownRenderBlock(id: id, kind: .thematicBreak)
        }
        if node is PageBreak {
            return MarkdownRenderBlock(id: id, kind: .pageBreak)
        }
        if let n = node as? Heading {
            return MarkdownRenderBlock(
                id: id,
                kind: .heading(HeadingBlock(
                    level: Int(n.level),
                    id: n.id,
                    inline: InlineAssembler.build(from: n)
                ))
            )
        }
        if let n = node as? SetextHeading {
            return MarkdownRenderBlock(
                id: id,
                kind: .setextHeading(HeadingBlock(
                    level: Int(n.level),
                    id: n.id,
                    inline: InlineAssembler.build(from: n)
                ))
            )
        }
        if let n = node as? Paragraph {
            return MarkdownRenderBlock(
                id: id,
                kind: .paragraph(ParagraphBlock(
                    inline: InlineAssembler.build(from: n),
                    blockAttributes: MarkdownKMPInterop.stringDict(from: n.blockAttributes)
                ))
            )
        }
        if let n = node as? FencedCodeBlock {
            let title = (n.attributes.pairs as [String: String])["title"]
            let f = FencedCodeBlockModel(
                language: n.language,
                info: n.info,
                code: n.literal,
                title: title,
                showLineNumbers: n.showLineNumbers,
                startLine: Int(n.startLineNumber),
                highlightedLines: MarkdownKMPInterop.intRangeList(from: n.highlightLines)
            )
            return MarkdownRenderBlock(id: id, kind: .fencedCode(f))
        }
        if let n = node as? IndentedCodeBlock {
            return MarkdownRenderBlock(id: id, kind: .indentedCode(n.literal))
        }
        if let n = node as? MathBlock {
            return MarkdownRenderBlock(id: id, kind: .mathBlock(n.literal))
        }
        if let n = node as? HtmlBlock {
            return MarkdownRenderBlock(id: id, kind: .htmlBlock(n.literal))
        }
        if let n = node as? FrontMatter {
            return MarkdownRenderBlock(id: id, kind: .frontMatter(n.literal, format: n.format))
        }
        if let n = node as? LinkReferenceDefinition {
            return MarkdownRenderBlock(
                id: id,
                kind: .linkReferenceDefinition(LinkRefDefBlock(
                    label: n.label,
                    destination: n.destination,
                    title: n.title
                ))
            )
        }
        if let n = node as? AbbreviationDefinition {
            return MarkdownRenderBlock(
                id: id,
                kind: .abbreviationDefinition(AbbreviationDefBlock(
                    abbreviation: n.abbreviation,
                    fullText: n.fullText
                ))
            )
        }
        if let n = node as? DiagramBlock {
            return MarkdownRenderBlock(
                id: id,
                kind: .diagram(DiagramBlockModel(type: n.diagramType, source: n.literal))
            )
        }
        if let n = node as? Figure {
            return MarkdownRenderBlock(
                id: id,
                kind: .figure(FigureBlock(
                    imageURL: n.imageUrl,
                    caption: n.caption,
                    width: MarkdownKMPInterop.optionalInt32(from: n.imageWidth),
                    height: MarkdownKMPInterop.optionalInt32(from: n.imageHeight),
                    attributes: MarkdownKMPInterop.stringDict(from: n.attributes)
                ))
            )
        }
        if let n = node as? Admonition {
            return MarkdownRenderBlock(
                id: id,
                kind: .admonition(AdmonitionBlock(
                    type: n.type,
                    title: n.title,
                    children: childBlocks(n, &ctx)
                ))
            )
        }
        if let n = node as? CustomContainer {
            let classes = n.cssClasses.map { String($0) }
            return MarkdownRenderBlock(
                id: id,
                kind: .customContainer(CustomContainerBlock(
                    type: n.type,
                    title: n.title,
                    cssClasses: classes,
                    cssId: n.cssId,
                    children: childBlocks(n, &ctx)
                ))
            )
        }
        if let n = node as? BlockQuote {
            return MarkdownRenderBlock(id: id, kind: .blockQuote(children: childBlocks(n, &ctx)))
        }
        if let n = node as? ListBlock {
            return MarkdownRenderBlock(id: id, kind: .list(listModel(from: n, &ctx)))
        }
        if let n = node as? Table {
            return MarkdownRenderBlock(id: id, kind: .table(extractTable(n)))
        }
        if let n = node as? FootnoteDefinition {
            return MarkdownRenderBlock(
                id: id,
                kind: .footnoteDefinition(FootnoteDefBlock(
                    label: n.label,
                    index: Int(n.index),
                    children: childBlocks(n, &ctx)
                ))
            )
        }
        if let n = node as? DefinitionList {
            return MarkdownRenderBlock(id: id, kind: .definitionList(items: buildDefinitionList(n, &ctx)))
        }
        if let n = node as? TocPlaceholder {
            var resolvedHeadings: [TocHeading] = ctx.catalog
                .filter { $0.level >= Int(n.minDepth) && $0.level <= Int(n.maxDepth) }
            if !n.excludeIds.isEmpty {
                let ex = Set(n.excludeIds.map { String($0) })
                resolvedHeadings = resolvedHeadings.filter { h in
                    guard let i = h.headingId else { return true }
                    return !ex.contains(i)
                }
            }
            if n.order == "desc" { resolvedHeadings.reverse() }
            let entries: [TocEntry] = resolvedHeadings.map { h in
                TocEntry(level: h.level, title: h.title, headingId: h.headingId)
            }
            let t = TocBlock(
                minDepth: Int(n.minDepth),
                maxDepth: Int(n.maxDepth),
                excludeIds: n.excludeIds.map { String($0) },
                order: n.order,
                resolvedEntries: entries
            )
            return MarkdownRenderBlock(id: id, kind: .toc(t))
        }
        if let n = node as? ColumnsLayout {
            let cols: [ColumnBlock] = n.children.compactMap { c -> ColumnBlock? in
                guard let col = c as? ColumnItem else { return nil }
                return ColumnBlock(
                    widthPercent: col.width.isEmpty ? nil : col.width,
                    children: childBlocks(col, &ctx)
                )
            }
            return MarkdownRenderBlock(id: id, kind: .columns(cols))
        }
        if let n = node as? DirectiveBlock {
            return MarkdownRenderBlock(
                id: id,
                kind: .directiveBlock(DirectiveBlockModel(
                    tag: n.tagName,
                    args: MarkdownKMPInterop.stringDict(from: n.args),
                    children: childBlocks(n, &ctx)
                ))
            )
        }
        if let n = node as? TabBlock {
            var tabs: [TabItemBlock] = []
            for c in n.children {
                guard let item = c as? TabItem else { continue }
                tabs.append(
                    TabItemBlock(title: item.title, children: childBlocks(item, &ctx))
                )
            }
            return MarkdownRenderBlock(id: id, kind: .tabBlock(tabs))
        }
        if let n = node as? BibliographyDefinition {
            var entries: [BibEntryBlock] = []
            let dict: NSDictionary = n.entries
            for case let k as String in dict.keyEnumerator() {
                if let b = dict.object(forKey: k) as? BibEntry {
                    entries.append(BibEntryBlock(key: b.key, content: b.content))
                }
            }
            return MarkdownRenderBlock(id: id, kind: .bibliography(entries))
        }

        return MarkdownRenderBlock(
            id: id,
            kind: .unsupportedNode(
                name: String(describing: type(of: node)),
                detail: "stableKey=\(node.stableKey)"
            )
        )
    }

    private static func childBlocks(_ node: ContainerNode, _ ctx: inout ConversionContext) -> [MarkdownRenderBlock] {
        var o: [MarkdownRenderBlock] = []
        for (i, ch) in node.children.enumerated() {
            if let b = convertBlock(ch, index: i, &ctx) {
                o.append(b)
            }
        }
        return o
    }

    private static func listModel(from list: ListBlock, _ ctx: inout ConversionContext) -> ListBlockModel {
        var items: [ListItemBlock] = []
        for c in list.children {
            guard let li = c as? ListItem else { continue }
            let ch = childBlocks(li, &ctx)
            items.append(
                ListItemBlock(
                    isTask: li.taskListItem,
                    checked: li.checked,
                    children: ch
                )
            )
        }
        return ListBlockModel(
            ordered: list.ordered,
            startNumber: Int(list.startNumber),
            bullet: Character(UnicodeScalar(UInt32(list.bulletChar))!),
            delimiter: Character(UnicodeScalar(UInt32(list.delimiter))!),
            tight: list.tight,
            items: items,
            blockAttributes: MarkdownKMPInterop.stringDict(from: list.blockAttributes)
        )
    }

    private static func buildDefinitionList(_ list: DefinitionList, _ ctx: inout ConversionContext) -> [DefinitionListItemBlock] {
        var out: [DefinitionListItemBlock] = []
        var i = 0
        let arr = list.children
        while i < arr.count {
            let c = arr[i]
            if let term = c as? DefinitionTerm {
                let termInline = InlineAssembler.build(from: term)
                i += 1
                var descBlocks: [MarkdownRenderBlock] = []
                if i < arr.count, let dd = arr[i] as? DefinitionDescription {
                    descBlocks = childBlocks(dd, &ctx)
                    i += 1
                }
                out.append(DefinitionListItemBlock(term: termInline, definitions: descBlocks))
            } else {
                i += 1
            }
        }
        return out
    }

    private static func extractTable(_ table: Table) -> TableBlock {
        var align: [TableBlock.ColumnAlignment] = table.columnAlignments.map { a in
            if a == .left { return .left }
            if a == .right { return .right }
            if a == .center { return .center }
            return .none
        }
        var header: [InlineContent]?
        var body: [[InlineContent]] = []
        if let head = table.children.first(where: { $0 is TableHead }) as? TableHead {
            if let row = head.children.compactMap({ $0 as? TableRow }).first {
                let cells = tableRowCells(row)
                if !cells.isEmpty { header = cells }
            }
        }
        if let tBody = table.children.first(where: { $0 is TableBody }) as? TableBody {
            for child in tBody.children {
                guard let row = child as? TableRow else { continue }
                let cells = tableRowCells(row)
                if !cells.isEmpty { body.append(cells) }
            }
        }
        let colCount = max(header?.count ?? 0, body.map(\.count).max() ?? 0, align.count)
        if colCount > 0, align.count < colCount {
            align.append(contentsOf: Array(repeating: .none, count: colCount - align.count))
        }
        return TableBlock(columnAlignment: align, headerRow: header, bodyRows: body)
    }

    private static func tableRowCells(_ row: TableRow) -> [InlineContent] {
        var cells: [InlineContent] = []
        for cell in row.children {
            if let tc = cell as? TableCell {
                cells.append(InlineAssembler.build(from: tc))
            }
        }
        return cells
    }
}

