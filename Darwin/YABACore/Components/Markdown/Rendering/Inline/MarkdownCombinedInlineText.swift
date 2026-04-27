//
//  MarkdownCombinedInlineText.swift
//  YABACore
//
//  Recursive `Text` assembly (no top-level images — those are handled by `MarkdownInlineBlockView`).
//

import SwiftUI

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
