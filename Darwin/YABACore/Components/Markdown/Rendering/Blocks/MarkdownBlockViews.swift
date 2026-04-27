//
//  MarkdownBlockViews.swift
//  YABACore
//
//  SwiftUI views for each `MarkdownRenderBlockKind`.
//

import SwiftUI
import WebKit
#if canImport(UIKit)
import UIKit
#endif

public struct MarkdownBlockStackView: View {
    public let blocks: [MarkdownRenderBlock]
    @Environment(\.markdownPreviewConfiguration) private var config
    public var listNesting: Int = 0

    public init(blocks: [MarkdownRenderBlock], listNesting: Int = 0) {
        self.blocks = blocks
        self.listNesting = listNesting
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(blocks) { block in
                MarkdownBlockView(block: block, configuration: config, listNesting: listNesting)
            }
        }
    }
}

public struct MarkdownBlockView: View {
    public let block: MarkdownRenderBlock
    public var configuration: MarkdownPreviewConfiguration
    /// Nesting of block lists; increments for nested `list` children (CommonMark / GFM).
    public var listNesting: Int

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.markdownTheme) private var themeOverride

    public init(
        block: MarkdownRenderBlock,
        configuration: MarkdownPreviewConfiguration,
        listNesting: Int = 0
    ) {
        self.block = block
        self.configuration = configuration
        self.listNesting = listNesting
    }

    public var body: some View {
        let theme = themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)
        content(theme: theme)
            .padding(.vertical, blockVerticalPadding)
    }

    private var blockVerticalPadding: CGFloat {
        switch block.kind {
        case .heading, .setextHeading: return 6
        case .thematicBreak, .pageBreak: return 8
        default: return 4
        }
    }

    @ViewBuilder
    private func content(theme: MarkdownThemeTokens) -> some View {
        switch block.kind {
        case .documentRoot(let children):
            MarkdownBlockStackView(blocks: children, listNesting: listNesting)
        case .heading(let h):
            headingView(h, theme: theme, isSetext: false)
        case .setextHeading(let h):
            headingView(h, theme: theme, isSetext: true)
        case .paragraph(let p):
            MarkdownInlineBlockView(content: p.inline)
                .frame(maxWidth: .infinity, alignment: .leading)
        case .thematicBreak:
            Divider()
        case .fencedCode(let c):
            fencedCodeView(c, theme: theme)
        case .indentedCode(let s):
            ScrollView(.horizontal, showsIndicators: true) {
                Text(s)
                    .font(theme.monospaced)
                    .textSelection(.enabled)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.codeBackground)
            .clipShape(RoundedRectangle(cornerRadius: 6))
        case .blockQuote(let ch):
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(theme.blockQuoteColor.opacity(0.5))
                    .frame(width: 3)
                MarkdownBlockStackView(blocks: ch, listNesting: listNesting)
            }
            .padding(8)
            .background(theme.codeBackground.opacity(0.3))
        case .list(let l):
            MarkdownListBlockView(list: l, configuration: configuration, listNesting: listNesting)
        case .htmlBlock(let raw):
            if configuration.useWebViewForHtmlBlocks {
                MarkdownHTMLBlockWebView(html: raw)
                    .frame(minHeight: 80)
            } else {
                ScrollView {
                    Text(raw)
                        .font(theme.monospaced)
                        .textSelection(.enabled)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        case .linkReferenceDefinition(let d):
            if configuration.showLinkReferenceBlocks {
                Text("[\(d.label)]: \(d.destination) \(d.title.map { " \"\($0)\"" } ?? "")")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            } else {
                EmptyView()
            }
        case .table(let t):
            MarkdownTableBlockView(table: t, theme: theme)
        case .footnoteDefinition(let f):
            VStack(alignment: .leading, spacing: 4) {
                Text("^\(f.label) (\(f.index))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                MarkdownBlockStackView(blocks: f.children, listNesting: listNesting)
            }
        case .mathBlock(let s):
            Text(s)
                .font(theme.monospaced)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(8)
                .background(theme.mathBackground)
        case .definitionList(let items):
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    VStack(alignment: .leading, spacing: 4) {
                        MarkdownInlineBlockView(content: item.term)
                            .fontWeight(.semibold)
                        MarkdownBlockStackView(blocks: item.definitions, listNesting: listNesting)
                    }
                }
            }
        case .admonition(let a):
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(a.type.uppercased())
                        .font(.caption2).fontWeight(.bold)
                    if !a.title.isEmpty { Text(a.title).font(.subheadline) }
                }
                MarkdownBlockStackView(blocks: a.children, listNesting: listNesting)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
        case .frontMatter(let s, _):
            if configuration.showFrontMatter {
                Text(s)
                    .font(theme.monospaced)
                    .textSelection(.enabled)
                    .padding(8)
                    .background(theme.codeBackground)
            } else {
                EmptyView()
            }
        case .toc(let t):
            VStack(alignment: .leading, spacing: 4) {
                if !t.resolvedEntries.isEmpty {
                    ForEach(t.resolvedEntries) { e in
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
        case .abbreviationDefinition:
            EmptyView()
        case .customContainer(let c):
            VStack(alignment: .leading, spacing: 6) {
                if !c.title.isEmpty { Text(c.title).fontWeight(.semibold) }
                MarkdownBlockStackView(blocks: c.children, listNesting: listNesting)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.secondary.opacity(0.05), in: RoundedRectangle(cornerRadius: 8))
        case .diagram(let d):
            VStack(alignment: .leading, spacing: 4) {
                Text("Diagram: \(d.type)")
                    .font(.headline)
                Text(d.source)
                    .font(.caption.monospaced())
                    .lineLimit(8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(
                (themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)).diagramFallbackBackground,
                in: RoundedRectangle(cornerRadius: 8)
            )
        case .columns(let cols):
            HStack(alignment: .top, spacing: 8) {
                ForEach(Array(cols.enumerated()), id: \.offset) { _, col in
                    MarkdownBlockStackView(blocks: col.children, listNesting: listNesting)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        case .pageBreak:
            HStack { Text("— page break —").font(.caption).foregroundColor(.secondary) }
        case .directiveBlock(let d):
            VStack(alignment: .leading, spacing: 4) {
                Text("::\(d.tag)").font(.caption.monospaced())
                if !d.args.isEmpty {
                    Text(d.args.map { "\($0.key)=\($0.value)" }.joined(separator: " "))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                MarkdownBlockStackView(blocks: d.children, listNesting: listNesting)
            }
        case .tabBlock(let tabs):
            if !tabs.isEmpty {
                MarkdownInDocumentTabView(tabs: tabs, configuration: configuration, listNesting: listNesting)
            } else { EmptyView() }
        case .bibliography(let b):
            VStack(alignment: .leading, spacing: 6) {
                Text("References").font(.headline)
                ForEach(Array(b.enumerated()), id: \.offset) { _, e in
                    HStack(alignment: .top) {
                        Text(e.key)
                            .fontWeight(.semibold)
                        Text(e.content)
                    }
                }
            }
        case .figure(let f):
            VStack(alignment: .center, spacing: 6) {
                MarkdownImageRowView(
                    urlString: f.imageURL,
                    alt: InlineContent(runs: [.text("Figure")]),
                    width: f.width,
                    height: f.height,
                    registry: configuration.assetRegistry
                )
                if !f.caption.isEmpty { Text(f.caption).font(.caption) }
            }
        case .metadataOmitted:
            EmptyView()
        case .unsupportedNode(let name, let detail):
            VStack(alignment: .leading, spacing: 2) {
                Text("Unsupported: \(name)")
                    .font(.caption)
                    .foregroundColor(.red)
                if let d = detail {
                    Text(d)
                        .font(.caption2.monospaced())
                        .foregroundColor(.secondary)
                }
            }
        }
    }

    private func headingView(_ h: HeadingBlock, theme: MarkdownThemeTokens, isSetext: Bool) -> some View {
        let font: Font = {
            switch h.level {
            case 1: return .largeTitle
            case 2: return .title
            case 3: return .title2
            case 4: return .title3
            case 5: return .headline
            default: return .subheadline
            }
        }()
        return VStack(alignment: .leading, spacing: 2) {
            MarkdownInlineBlockView(content: h.inline)
                .font(font)
                .fontWeight(h.level <= 2 ? .bold : .semibold)
            if isSetext {
                if h.level <= 1 { Divider() }
            }
        }
    }

    private func fencedCodeView(_ c: FencedCodeBlockModel, theme: MarkdownThemeTokens) -> some View {
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
}

// MARK: - List

struct MarkdownListBlockView: View {
    let list: ListBlockModel
    let configuration: MarkdownPreviewConfiguration
    var listNesting: Int

    var body: some View {
        VStack(alignment: .leading, spacing: list.tight ? 0 : 4) {
            ForEach(Array(list.items.enumerated()), id: \.offset) { idx, item in
                MarkdownListItemView(
                    item: item,
                    index: list.ordered ? (list.startNumber + idx) : nil,
                    bullet: list.bullet,
                    ordered: list.ordered,
                    configuration: configuration,
                    listNesting: listNesting
                )
            }
        }
        .padding(.leading, CGFloat(listNesting) * 16)
    }
}

struct MarkdownListItemView: View {
    let item: ListItemBlock
    let index: Int?
    let bullet: Character
    let ordered: Bool
    let configuration: MarkdownPreviewConfiguration
    var listNesting: Int

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            listMarker
            VStack(alignment: .leading, spacing: 4) {
                ForEach(item.children) { child in
                    if case .list(let inner) = child.kind {
                        MarkdownListBlockView(
                            list: inner,
                            configuration: configuration,
                            listNesting: listNesting + 1
                        )
                    } else {
                        MarkdownBlockView(
                            block: child,
                            configuration: configuration,
                            listNesting: listNesting
                        )
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var listMarker: some View {
        if item.isTask {
            Image(systemName: item.checked ? "checkmark.square.fill" : "square")
                .foregroundColor(.secondary)
        } else if ordered, let n = index {
            Text("\(n).")
        } else {
            Text(verbatim: String(bullet))
        }
    }
}

// MARK: - Table (grid)

struct MarkdownTableBlockView: View {
    let table: TableBlock
    let theme: MarkdownThemeTokens

    var body: some View {
        ScrollView([.vertical, .horizontal], showsIndicators: true) {
            VStack(alignment: .leading, spacing: 0) {
                if let header = table.headerRow {
                    rowView(cells: header, isHeader: true)
                    Divider()
                }
                ForEach(Array(table.bodyRows.enumerated()), id: \.offset) { i, r in
                    rowView(cells: r, isHeader: false)
                    if i < table.bodyRows.count - 1 { Divider() }
                }
            }
        }
    }

    private func rowView(cells: [InlineContent], isHeader: Bool) -> some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Array(cells.enumerated()), id: \.offset) { cIdx, cell in
                let align = cIdx < table.columnAlignment.count
                    ? table.columnAlignment[cIdx]
                    : .none
                MarkdownInlineBlockView(content: cell)
                    .font(isHeader ? .subheadline.weight(.semibold) : .subheadline)
                    .frame(maxWidth: 180, alignment: cellAlignment(align))
                    .padding(6)
                    .frame(maxWidth: .infinity, alignment: cellAlignment(align))
                if cIdx < cells.count - 1 {
                    Rectangle()
                        .fill(Color.secondary.opacity(0.25))
                        .frame(width: 1)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isHeader ? Color.gray.opacity(0.14) : Color.clear)
    }

    /// `frame(alignment:)` takes `Alignment`, not `HorizontalAlignment`.
    private func cellAlignment(_ a: TableBlock.ColumnAlignment) -> Alignment {
        switch a {
        case .left: return .leading
        case .center: return .center
        case .right: return .trailing
        case .none: return .leading
        }
    }
}

// MARK: - In-document tabs (not SwiftUI `TabView` pages)

struct MarkdownInDocumentTabView: View {
    let tabs: [TabItemBlock]
    let configuration: MarkdownPreviewConfiguration
    var listNesting: Int
    @State private var index: Int = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if tabs.count <= 4 {
                Picker("Tab", selection: $index) {
                    ForEach(tabs.indices, id: \.self) { i in
                        Text(tabs[i].title).lineLimit(1).tag(i)
                    }
                }
                .pickerStyle(.segmented)
            } else {
                Picker("Tab", selection: $index) {
                    ForEach(tabs.indices, id: \.self) { i in
                        Text(tabs[i].title).tag(i)
                    }
                }
            }
            if tabs.indices.contains(index) {
                MarkdownBlockStackView(blocks: tabs[index].children, listNesting: listNesting)
            }
        }
    }
}

// MARK: - HTML WebView

#if canImport(UIKit)
struct MarkdownHTMLBlockWebView: UIViewRepresentable {
    let html: String

    func makeUIView(context: Context) -> WKWebView {
        let c = WKWebViewConfiguration()
        c.defaultWebpagePreferences.allowsContentJavaScript = true
        let w = WKWebView(frame: .zero, configuration: c)
        w.isOpaque = false
        w.scrollView.isScrollEnabled = true
        return w
    }

    func updateUIView(_ w: WKWebView, context: Context) {
        w.loadHTMLString(MarkdownHTMLBlockTemplate.htmlDocument(html: html), baseURL: nil)
    }
}
#elseif canImport(AppKit)
import AppKit
struct MarkdownHTMLBlockWebView: NSViewRepresentable {
    let html: String

    func makeNSView(context: Context) -> WKWebView {
        let c = WKWebViewConfiguration()
        c.defaultWebpagePreferences.allowsContentJavaScript = true
        let w = WKWebView(frame: .zero, configuration: c)
        return w
    }

    func updateNSView(_ w: WKWebView, context: Context) {
        w.loadHTMLString(MarkdownHTMLBlockTemplate.htmlDocument(html: html), baseURL: nil)
    }
}
#endif

private enum MarkdownHTMLBlockTemplate {
    static func htmlDocument(html: String) -> String {
        """
        <!doctype html><html><head><meta name=viewport content="width=device-width,initial-scale=1">
        <style>body { font: -apple-system-body; }
        pre { white-space: pre-wrap; } code { font-family: ui-monospace; }</style>
        </head><body>
        """ + html + "</body></html>"
    }
}
