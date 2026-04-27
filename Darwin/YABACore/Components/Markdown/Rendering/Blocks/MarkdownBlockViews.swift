//
//  MarkdownBlockViews.swift
//  YABACore
//
//  SwiftUI router for `MarkdownRenderBlock` and shared heading / fenced code helpers.
//

import SwiftUI
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
                MarkdownBlockView(
                    block: block,
                    configuration: config,
                    listNesting: listNesting
                )
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
            Divider().frame(height: 2)
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
