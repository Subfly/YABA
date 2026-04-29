//
//  MarkdownCombinedInlineText.swift
//  YABACore
//
//  Rich inline rendering. On platforms with UIKit we use a selectable `UITextView`-backed
//  `NSAttributedString` because SwiftUI `Text` + `textSelection(.enabled)` does not show conventional
//  selection highlighting.
//

import SwiftUI

struct MarkdownCombinedInlineText: View {
    let content: InlineContent
    var typography: MarkdownInlineTypography = .body
    let theme: MarkdownThemeTokens
    let onLink: (URL) -> Void

    @Environment(\.colorScheme) private var colorScheme

    init(
        content: InlineContent,
        typography: MarkdownInlineTypography = .body,
        theme: MarkdownThemeTokens,
        onLink: @escaping (URL) -> Void = { _ in }
    ) {
        self.content = content
        self.typography = typography
        self.theme = theme
        self.onLink = onLink
    }

    var body: some View {
#if canImport(UIKit)
        MarkdownSelectableTextRepresentable(
            attributedText: MarkdownInlineAttributedStringBuilder.attributedString(
                for: content,
                typography: typography,
                theme: theme,
                colorScheme: colorScheme
            ),
            textAlignment: .natural
        )
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity, alignment: .leading)
#else
        legacyReducedText.markdownInlineTypography(typography).textSelection(.enabled)
#endif
    }

#if !canImport(UIKit)
    private var legacyReducedText: Text {
        content.runs.reduce(Text("")) { partial, r in
            partial + runTextSwiftUI(r)
        }
    }

    private func combinedSwiftUI(_ c: InlineContent) -> Text {
        c.runs.reduce(Text("")) { partial, r in
            partial + runTextSwiftUI(r)
        }
    }

    private func runTextSwiftUI(_ run: InlineRun) -> Text {
        switch run {
        case let .text(t): return Text(t)
        case let .lineBreak(soft): return Text(soft ? " " : "\n")
        case let .code(t): return Text(t).font(theme.monospaced)
        case let .emphasis(c): return combinedSwiftUI(c).italic()
        case let .strong(c): return combinedSwiftUI(c).bold()
        case let .strikethrough(c): return combinedSwiftUI(c).strikethrough()
        case let .link(c, _, _):
            return combinedSwiftUI(c).foregroundColor(theme.linkColor).underline()
        case let .autolink(_, _, display):
            return Text(display).foregroundColor(theme.linkColor).underline()
        case let .image(alt, _, _, _, _, _):
            return Text("[") + combinedSwiftUI(alt) + Text("]")
        case let .footnoteRef(_, idx):
            return Text("[\(idx)]").font(.caption).foregroundColor(.secondary)
        case let .mathInline(t): return Text(t).font(theme.monospaced)
        case let .highlight(c): return combinedSwiftUI(c)
        case let .superscript(c): return combinedSwiftUI(c).font(.caption)
        case let .subscripted(c): return combinedSwiftUI(c).font(.caption2)
        case let .inserted(c): return combinedSwiftUI(c).underline()
        case let .emoji(_, u): return Text(u.map { String($0) } ?? "")
        case let .styled(c, _): return combinedSwiftUI(c)
        case let .abbreviation(short, _): return Text(short)
        case let .kbd(t): return Text(t).font(theme.monospaced)
        case let .citation(key): return Text("[\(key)]")
        case let .spoiler(c): return combinedSwiftUI(c).foregroundColor(.primary.opacity(0.2))
        case let .wikiLink(_, label):
            return Text(label ?? "").foregroundColor(theme.linkColor)
        case let .ruby(b, ann): return Text("\(b) (\(ann))")
        case let .htmlInline(h):
            return Text(h).font(theme.monospaced).foregroundColor(.secondary)
        case let .directiveInline(tag, _):
            return Text(":\(tag):").font(theme.monospaced).foregroundColor(.secondary)
        }
    }
#endif
}

#if !canImport(UIKit)
extension Text {
    fileprivate func markdownInlineTypography(_ t: MarkdownInlineTypography) -> some View {
        switch t {
        case .body:
            return AnyView(self)
        case let .heading(level):
            let font: Font =
                switch level {
                case 1: .largeTitle
                case 2: .title
                case 3: .title2
                case 4: .title3
                case 5: .headline
                default: .subheadline
                }
            let w: Font.Weight = level <= 2 ? .bold : .semibold
            return AnyView(self.font(font).fontWeight(w))
        case .definitionTerm:
            return AnyView(self.fontWeight(.semibold))
        case .tableCell:
            return AnyView(self.font(.subheadline))
        }
    }
}
#endif
