//
//  MarkdownKMPNodeBlockViews.swift
//  YABACore
//
//  Renders KMP `Node` values directly (no `MarkdownASTConverter` / `MarkdownRenderBlock`).
//

import Foundation
import MarkdownParser
import SwiftUI

// MARK: - Public stack (document / nested containers)

public struct MarkdownKMPBlockStackView: View {
    public let nodes: [Node]
    public var listNesting: Int = 0
    @Environment(\.markdownPreviewConfiguration) private var config

    public init(document: Document, listNesting: Int = 0) {
        self.init(nodes: (document as ContainerNode).children, listNesting: listNesting)
    }

    public init(container: ContainerNode, listNesting: Int = 0) {
        self.init(nodes: container.children, listNesting: listNesting)
    }

    public init(nodes: [Node], listNesting: Int = 0) {
        self.nodes = nodes
        self.listNesting = listNesting
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(filteredRowModels(), id: \.id) { row in
                MarkdownKMPNodeBlockView(
                    node: row.node,
                    tableIndex: row.sourceIndex,
                    listNesting: listNesting
                )
            }
        }
    }

    private func filteredRowModels() -> [KMPBlockRow] {
        var o: [KMPBlockRow] = []
        var j = 0
        for (i, n) in nodes.enumerated() {
            if n is BlankLine { continue }
            o.append(KMPBlockRow(id: kmpRowIdentifier(index: j, node: n), sourceIndex: i, node: n))
            j += 1
        }
        return o
    }
}

private struct KMPBlockRow: Identifiable {
    let id: String
    let sourceIndex: Int
    let node: Node
}

// MARK: - Single block

public struct MarkdownKMPNodeBlockView: View {
    public let node: Node
    public var tableIndex: Int
    public var listNesting: Int

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.markdownTheme) private var themeOverride
    @Environment(\.markdownPreviewConfiguration) private var configuration
    @Environment(\.markdownTocHeadings) private var tocHeadings

    public init(node: Node, tableIndex: Int, listNesting: Int) {
        self.node = node
        self.tableIndex = tableIndex
        self.listNesting = listNesting
    }

    public var body: some View {
        let theme = themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)
        content(theme: theme)
            .padding(.vertical, verticalPadding)
    }

    private var verticalPadding: CGFloat {
        if node is Heading || node is SetextHeading { return 6 }
        if node is ThematicBreak || node is PageBreak { return 8 }
        return 4
    }

    @ViewBuilder
    private func content(theme: MarkdownThemeTokens) -> some View {
        if node is BlankLine {
            EmptyView()
        } else if node is ThematicBreak {
            Divider()
        } else if node is PageBreak {
            HStack { Text("— page break —").font(.caption).foregroundColor(.secondary) }
        } else if let n = node as? Heading {
            headingView(level: Int(n.level), id: n.id, container: n, isSetext: false, theme: theme)
        } else if let n = node as? SetextHeading {
            headingView(level: Int(n.level), id: n.id, container: n, isSetext: true, theme: theme)
        } else if let n = node as? Paragraph {
            MarkdownInlineBlockView(content: InlineAssembler.build(from: n))
                .frame(maxWidth: .infinity, alignment: .leading)
        } else if let n = node as? FencedCodeBlock {
            fencedCodeView(from: n, theme: theme)
        } else if let n = node as? IndentedCodeBlock {
            ScrollView(.horizontal, showsIndicators: true) {
                Text(n.literal)
                    .font(theme.monospaced)
                    .textSelection(.enabled)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.codeBackground)
            .clipShape(RoundedRectangle(cornerRadius: 6))
        } else if let n = node as? MathBlock {
            Text(n.literal)
                .font(theme.monospaced)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(8)
                .background(theme.mathBackground)
        } else if let n = node as? HtmlBlock {
            if configuration.useWebViewForHtmlBlocks {
                MarkdownHTMLBlockWebView(html: n.literal)
                    .frame(minHeight: 80)
            } else {
                ScrollView {
                    Text(n.literal)
                        .font(theme.monospaced)
                        .textSelection(.enabled)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else if let n = node as? LinkReferenceDefinition {
            if configuration.showLinkReferenceBlocks {
                Text("[\(n.label)]: \(n.destination) \(n.title.map { " \"\($0)\"" } ?? "")")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            } else { EmptyView() }
        } else if let n = node as? MarkdownKMPTableNode {
            MarkdownTableBlockView(table: MarkdownKMPTableSupport.tableBlock(from: n), theme: theme)
        } else if let n = node as? BlockQuote {
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(theme.blockQuoteColor.opacity(0.5))
                    .frame(width: 3)
                MarkdownKMPBlockStackView(container: n, listNesting: listNesting)
            }
            .padding(8)
            .background(theme.codeBackground.opacity(0.3))
        } else if let n = node as? ListBlock {
            MarkdownKMPListBlockView(list: n, listNesting: listNesting)
        } else if let n = node as? FootnoteDefinition {
            VStack(alignment: .leading, spacing: 4) {
                Text("^\(n.label) (\(n.index))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                MarkdownKMPBlockStackView(container: n, listNesting: listNesting)
            }
        } else if let n = node as? DefinitionList {
            definitionListView(n)
        } else if let n = node as? Admonition {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(n.type.uppercased())
                        .font(.caption2).fontWeight(.bold)
                    if !n.title.isEmpty { Text(n.title).font(.subheadline) }
                }
                MarkdownKMPBlockStackView(container: n, listNesting: listNesting)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
        } else if let n = node as? FrontMatter {
            if configuration.showFrontMatter {
                Text(n.literal)
                    .font(theme.monospaced)
                    .textSelection(.enabled)
                    .padding(8)
                    .background(theme.codeBackground)
            } else { EmptyView() }
        } else if let n = node as? TocPlaceholder {
            tocView(from: n)
        } else if node is AbbreviationDefinition {
            EmptyView()
        } else if let n = node as? CustomContainer {
            VStack(alignment: .leading, spacing: 6) {
                if !n.title.isEmpty { Text(n.title).fontWeight(.semibold) }
                MarkdownKMPBlockStackView(container: n, listNesting: listNesting)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.secondary.opacity(0.05), in: RoundedRectangle(cornerRadius: 8))
        } else if let n = node as? DiagramBlock {
            VStack(alignment: .leading, spacing: 4) {
                Text("Diagram: \(n.diagramType)")
                    .font(.headline)
                Text(n.literal)
                    .font(.caption.monospaced())
                    .lineLimit(8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(
                theme.diagramFallbackBackground,
                in: RoundedRectangle(cornerRadius: 8)
            )
        } else if let n = node as? ColumnsLayout {
            HStack(alignment: .top, spacing: 8) {
                ForEach(Array(n.children.enumerated()), id: \.offset) { _, ch in
                    if let col = ch as? ColumnItem {
                        MarkdownKMPBlockStackView(container: col, listNesting: listNesting)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        } else if let n = node as? DirectiveBlock {
            VStack(alignment: .leading, spacing: 4) {
                Text("::\(n.tagName)").font(.caption.monospaced())
                let args = MarkdownKMPStringDictBridge.dict(n.args)
                if !args.isEmpty {
                    Text(args.map { "\($0.key)=\($0.value)" }.joined(separator: " "))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                MarkdownKMPBlockStackView(container: n, listNesting: listNesting)
            }
        } else if let n = node as? TabBlock {
            kmpTabView(n)
        } else if let n = node as? BibliographyDefinition {
            bibliographyDefinitionView(n)
        } else if let n = node as? Figure {
            VStack(alignment: .center, spacing: 6) {
                MarkdownImageRowView(
                    urlString: n.imageUrl,
                    alt: InlineContent(runs: [.text("Figure")]),
                    width: MarkdownKMPInterop.optionalInt32(from: n.imageWidth),
                    height: MarkdownKMPInterop.optionalInt32(from: n.imageHeight),
                    registry: configuration.assetRegistry
                )
                if !n.caption.isEmpty { Text(n.caption).font(.caption) }
            }
        } else {
            unsupported
        }
    }

    private func bibliographyDefinitionView(_ n: BibliographyDefinition) -> some View {
        let entries = kmpBibEntryBlocks(from: n)
        return VStack(alignment: .leading, spacing: 6) {
            Text("References").font(.headline)
            ForEach(Array(entries.enumerated()), id: \.offset) { _, e in
                HStack(alignment: .top) {
                    Text(e.key)
                        .fontWeight(.semibold)
                    Text(e.content)
                }
            }
        }
    }

    private func kmpBibEntryBlocks(from n: BibliographyDefinition) -> [BibEntryBlock] {
        var entries: [BibEntryBlock] = []
        if let d = n.entries as? NSDictionary {
            for case let k as String in d.keyEnumerator() {
                if let b = d.object(forKey: k) as? BibEntry {
                    entries.append(BibEntryBlock(key: b.key, content: b.content))
                }
            }
        }
        return entries
    }

    @ViewBuilder
    private var unsupported: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Unsupported: \(String(describing: type(of: node)))")
                .font(.caption)
                .foregroundColor(.red)
            Text("stableKey=\(node.stableKey)")
                .font(.caption2.monospaced())
                .foregroundColor(.secondary)
        }
    }

    // MARK: Pieces

    private func headingView(
        level: Int,
        id _: String?,
        container: ContainerNode,
        isSetext: Bool,
        theme: MarkdownThemeTokens
    ) -> some View {
        let font: Font = {
            switch level {
            case 1: return .largeTitle
            case 2: return .title
            case 3: return .title2
            case 4: return .title3
            case 5: return .headline
            default: return .subheadline
            }
        }()
        return VStack(alignment: .leading, spacing: 2) {
            MarkdownInlineBlockView(content: InlineAssembler.build(from: container))
                .font(font)
                .fontWeight(level <= 2 ? .bold : .semibold)
            if isSetext, level <= 1 { Divider() }
        }
    }

    private func fencedCodeView(from n: FencedCodeBlock, theme: MarkdownThemeTokens) -> some View {
        let title = MarkdownKMPStringDictBridge.dict(n.attributes.pairs)["title"]
        let c = FencedCodeBlockModel(
            language: n.language,
            info: n.info,
            code: n.literal,
            title: title,
            showLineNumbers: n.showLineNumbers,
            startLine: Int(n.startLineNumber),
            highlightedLines: MarkdownKMPInterop.intRangeList(from: n.highlightLines)
        )
        return fencedCodeModelView(c, theme: theme)
    }

    private func fencedCodeModelView(_ c: FencedCodeBlockModel, theme: MarkdownThemeTokens) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                if !c.info.isEmpty { Text(c.info).font(.caption).foregroundColor(.secondary) }
                if let t = c.title, !t.isEmpty { Text(t).font(.caption) }
            }
            ScrollView([.vertical, .horizontal], showsIndicators: true) {
                if c.showLineNumbers {
                    HStack(alignment: .top, spacing: 8) {
                        VStack(alignment: .trailing) {
                            ForEach(Array(c.code.split(separator: "\n", omittingEmptySubsequences: false).enumerated()), id: \.offset) { i, _ in
                                Text("\(c.startLine + i)").font(.caption2.monospaced())
                                    .foregroundColor(.secondary)
                            }
                        }
                        Text(c.code)
                            .font(theme.monospaced)
                            .textSelection(.enabled)
                    }
                } else {
                    Text(c.code)
                        .font(theme.monospaced)
                        .textSelection(.enabled)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(theme.codeBackground, in: RoundedRectangle(cornerRadius: 6))
    }

    private func definitionListView(_ list: DefinitionList) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(kmpDefinitionListItems(list).enumerated()), id: \.offset) { _, item in
                VStack(alignment: .leading, spacing: 4) {
                    MarkdownInlineBlockView(content: item.term)
                        .fontWeight(.semibold)
                    MarkdownKMPBlockStackView(nodes: item.definitionNodes, listNesting: listNesting)
                }
            }
        }
    }

    private struct KMPDefListRow {
        let term: InlineContent
        let definitionNodes: [Node]
    }

    private func kmpDefinitionListItems(_ list: DefinitionList) -> [KMPDefListRow] {
        var out: [KMPDefListRow] = []
        var i = 0
        let arr = list.children
        while i < arr.count {
            let c = arr[i]
            if let term = c as? DefinitionTerm {
                let termInline = InlineAssembler.build(from: term)
                i += 1
                var defNodes: [Node] = []
                if i < arr.count, let dd = arr[i] as? DefinitionDescription {
                    defNodes = (dd as ContainerNode).children.filter { !($0 is BlankLine) }
                    i += 1
                }
                out.append(KMPDefListRow(term: termInline, definitionNodes: defNodes))
            } else {
                i += 1
            }
        }
        return out
    }

    private func tocView(from n: TocPlaceholder) -> some View {
        var resolved = tocHeadings
            .filter { $0.level >= Int(n.minDepth) && $0.level <= Int(n.maxDepth) }
        if !n.excludeIds.isEmpty {
            let ex = Set(n.excludeIds.map { String($0) })
            resolved = resolved.filter { h in
                guard let i = h.headingId else { return true }
                return !ex.contains(i)
            }
        }
        if n.order == "desc" { resolved.reverse() }
        let entries: [TocEntry] = resolved.map { h in
            TocEntry(level: h.level, title: h.title, headingId: h.headingId)
        }
        return VStack(alignment: .leading, spacing: 4) {
            if !entries.isEmpty {
                ForEach(entries) { e in
                    HStack(alignment: .firstTextBaseline) {
                        Text(String(repeating: "  ", count: max(0, e.level - 1)))
                        Text("• " + e.title)
                            .font(e.level <= 1 ? .body : .subheadline)
                    }
                }
            } else {
                Text("Table of contents")
                    .font(.headline)
            }
        }
    }

    @ViewBuilder
    private func kmpTabView(_ n: TabBlock) -> some View {
        let tabs = collectKmpTabs(n)
        if tabs.isEmpty {
            EmptyView()
        } else {
            MarkdownKMPInDocumentTabView(tabs: tabs, listNesting: listNesting)
        }
    }
}
