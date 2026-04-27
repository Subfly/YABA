//
//  MarkdownInlineViews.swift
//  YABACore
//
//  SwiftUI rendering for inline content (recursive Text + image segments).
//

import SwiftUI

private struct MarkdownEnvironmentKey: EnvironmentKey {
    static let defaultValue: MarkdownThemeTokens? = nil
}

private struct MarkdownPreviewConfigurationKey: EnvironmentKey {
    static let defaultValue: MarkdownPreviewConfiguration = .init()
}

public struct MarkdownTocHeadingsKey: EnvironmentKey {
    public static let defaultValue: [MarkdownTocHeading] = []
}

extension EnvironmentValues {
    public var markdownTheme: MarkdownThemeTokens? {
        get { self[MarkdownEnvironmentKey.self] }
        set { self[MarkdownEnvironmentKey.self] = newValue }
    }

    public var markdownPreviewConfiguration: MarkdownPreviewConfiguration {
        get { self[MarkdownPreviewConfigurationKey.self] }
        set { self[MarkdownPreviewConfigurationKey.self] = newValue }
    }

    /// Collected from the current KMP `Document` for `[TOC]` / `TocPlaceholder` resolution.
    public var markdownTocHeadings: [MarkdownTocHeading] {
        get { self[MarkdownTocHeadingsKey.self] }
        set { self[MarkdownTocHeadingsKey.self] = newValue }
    }
}

public extension View {
    func markdownTheme(_ theme: MarkdownThemeTokens?) -> some View {
        environment(\.markdownTheme, theme)
    }

    func markdownPreviewConfiguration(_ config: MarkdownPreviewConfiguration) -> some View {
        environment(\.markdownPreviewConfiguration, config)
    }

    func markdownTocHeadings(_ headings: [MarkdownTocHeading]) -> some View {
        environment(\.markdownTocHeadings, headings)
    }
}

// MARK: - Paragraph pieces (text vs image at top level)

enum InlineDisplayPiece {
    case text(InlineContent)
    case image(alt: InlineContent, url: String, title: String?, width: Int?, height: Int?, attributes: KMPStringDict)
}

func splitInlineForDisplay(_ content: InlineContent) -> [InlineDisplayPiece] {
    var out: [InlineDisplayPiece] = []
    var buf: [InlineRun] = []
    for r in content.runs {
        if case let .image(alt, url, title, w, h, attr) = r {
            if !buf.isEmpty {
                out.append(.text(InlineContent(runs: buf)))
                buf = []
            }
            out.append(.image(alt: alt, url: url, title: title, width: w, height: h, attributes: attr))
        } else {
            buf.append(r)
        }
    }
    if !buf.isEmpty {
        out.append(.text(InlineContent(runs: buf)))
    }
    if out.isEmpty, content.runs.isEmpty { return [] }
    if out.isEmpty { return [.text(content)] }
    return out
}

// MARK: - Main inline stack

struct MarkdownInlineBlockView: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.markdownTheme) private var themeOverride
    @Environment(\.markdownPreviewConfiguration) private var config

    let content: InlineContent

    var body: some View {
        let theme = themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)
        let pieces = splitInlineForDisplay(content)
        if pieces.isEmpty { EmptyView() } else {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(Array(pieces.enumerated()), id: \.offset) { _, piece in
                    switch piece {
                    case .text(let c):
                        MarkdownCombinedInlineText(
                            content: c,
                            theme: theme
                        ) { _ in }
                    case .image(let alt, let url, _, let w, let h, _):
                        VStack(alignment: .leading, spacing: 4) {
                            MarkdownImageRowView(
                                urlString: url,
                                alt: alt,
                                width: w,
                                height: h,
                                registry: config.assetRegistry
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Combined Text (no top-level images)

struct MarkdownCombinedInlineText: View {
    let content: InlineContent
    let theme: MarkdownThemeTokens
    let onLink: (URL) -> Void

    var body: some View {
        content.runs.reduce(Text("")) { partial, r in
            partial + runText(r, theme: theme, onLink: onLink)
        }
        .textSelection(.enabled)
    }

    private func runText(
        _ run: InlineRun,
        theme: MarkdownThemeTokens,
        onLink: @escaping (URL) -> Void
    ) -> Text {
        switch run {
        case .text(let t):
            return Text(t)
        case .lineBreak(let soft):
            return Text(soft ? " " : "\n")
        case .code(let t):
            return Text(t).font(theme.monospaced)
        case .emphasis(let c):
            return combinedText(c, theme: theme, onLink: onLink).italic()
        case .strong(let c):
            return combinedText(c, theme: theme, onLink: onLink).bold()
        case .strikethrough(let c):
            return combinedText(c, theme: theme, onLink: onLink).strikethrough()
        case .link(let c, let url, _):
            return combinedText(c, theme: theme, onLink: onLink)
                .foregroundColor(theme.linkColor)
                .underline()
        case .autolink(_, _, let display):
            return Text(display)
                .foregroundColor(theme.linkColor)
                .underline()
        case .image(let alt, _, _, _, _, _):
            return Text("[")
                + combinedText(alt, theme: theme, onLink: onLink)
                + Text("]")
        case .footnoteRef(_, let index):
            return Text("[\(index)]").font(.caption).foregroundColor(.secondary)
        case .mathInline(let t):
            return Text(t).font(theme.monospaced)
        case .highlight(let c):
            return combinedText(c, theme: theme, onLink: onLink)
        case .superscript(let c):
            return combinedText(c, theme: theme, onLink: onLink).font(.caption)
        case .subscripted(let c):
            return combinedText(c, theme: theme, onLink: onLink).font(.caption2)
        case .inserted(let c):
            return combinedText(c, theme: theme, onLink: onLink).underline()
        case .emoji(_, let u):
            return Text(u.map { String($0) } ?? "")
        case .styled(let c, _):
            return combinedText(c, theme: theme, onLink: onLink)
        case .abbreviation(let short, _):
            return Text(short)
        case .kbd(let t):
            return Text(t).font(theme.monospaced)
        case .citation(let key):
            return Text("[\(key)]")
        case .spoiler(let c):
            return combinedText(c, theme: theme, onLink: onLink)
                .foregroundColor(.primary.opacity(0.2))
        case .wikiLink(_, let label):
            return Text(label ?? "")
                .foregroundColor(theme.linkColor)
        case .ruby(let b, let ann):
            return Text("\(b) (\(ann))")
        case .htmlInline(let h):
            return Text(h)
                .font(theme.monospaced)
                .foregroundColor(.secondary)
        case .directiveInline(let tag, _):
            return Text(":\(tag):")
                .font(theme.monospaced)
                .foregroundColor(.secondary)
        }
    }

    private func combinedText(
        _ c: InlineContent,
        theme: MarkdownThemeTokens,
        onLink: @escaping (URL) -> Void
    ) -> Text {
        c.runs.reduce(Text("")) { partial, r in
            partial + runText(r, theme: theme, onLink: onLink)
        }
    }
}
