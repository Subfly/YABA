//
//  MarkdownInlineAttributedStringBuilder.swift
//  YABACore
//
//  UIKit selectable text uses NSAttributedString; this mirrors `MarkdownCombinedInlineText` styling.
//

#if canImport(UIKit)
import SwiftUI
import UIKit

extension MarkdownInlineTypography {
    func baseUIFont() -> UIFont {
        switch self {
        case .body, .definitionTerm:
            UIFont.preferredFont(forTextStyle: .body)
        case let .heading(level):
            headingUIFont(level: level)
        case .tableCell:
            UIFont.preferredFont(forTextStyle: .subheadline)
        }
    }

    func headingWeight(forLevel level: Int) -> UIFont.Weight {
        switch self {
        case .heading: level <= 2 ? .bold : .semibold
        case .definitionTerm: .semibold
        default: .regular
        }
    }

    private func headingUIFont(level: Int) -> UIFont {
        let style: UIFont.TextStyle =
            switch level {
            case 1: .largeTitle
            case 2: .title1
            case 3: .title2
            case 4: .title3
            case 5: .headline
            default: .subheadline
            }
        return UIFont.preferredFont(forTextStyle: style)
    }
}

// MARK: - Theme UIColors

enum MarkdownSelectableThemeUIKit {
    static func label() -> UIColor { .label }

    static func secondaryLabel() -> UIColor { .secondaryLabel }

    static func spoilerForeground() -> UIColor { .label.withAlphaComponent(0.2) }

    static func linkColor(for colorScheme: ColorScheme) -> UIColor {
        colorScheme == .dark
            ? UIColor(red: 0.35, green: 0.6, blue: 1, alpha: 1)
            : UIColor(red: 0.04, green: 0.4, blue: 0.85, alpha: 1)
    }
}

// MARK: - Builder

private struct InlineAttrState: Sendable {
    var italic = false
    var bold = false
    var underline = false
    var strikethrough = false
    var superscript = false
    var subscripted = false
}

enum MarkdownInlineAttributedStringBuilder {
    static func attributedString(
        for content: InlineContent,
        typography: MarkdownInlineTypography,
        theme _: MarkdownThemeTokens,
        colorScheme: ColorScheme
    ) -> NSAttributedString {
        let base = typography.baseUIFont()
        let resolvedRoot: UIFont =
            switch typography {
            case let .heading(lvl):
                weightedSystemFont(
                    size: base.pointSize,
                    weight: typography.headingWeight(forLevel: lvl)
                )
            case .definitionTerm:
                weightedSystemFont(size: base.pointSize, weight: .semibold)
            default:
                base
            }

        let metricsTextStyle: UIFont.TextStyle =
            typography == .tableCell ? .subheadline : .body
        let monoSize = UIFontMetrics(forTextStyle: metricsTextStyle).scaledValue(
            for: UIFont.monospacedSystemFont(ofSize: 15, weight: .regular).pointSize
        )
        let monospace = UIFont.monospacedSystemFont(ofSize: monoSize, weight: .regular)

        let out = NSMutableAttributedString()
        recurse(
            content,
            into: out,
            state: InlineAttrState(),
            basePointSize: resolvedRoot.pointSize,
            monospaceFont: monospace,
            colorScheme: colorScheme,
            foreground: MarkdownSelectableThemeUIKit.label(),
            superscriptRaiseFactor: CGFloat(0.35),
            superscriptShrink: CGFloat(0.8)
        )
        return out
    }

    private static func recurse(
        _ content: InlineContent,
        into out: NSMutableAttributedString,
        state: InlineAttrState,
        basePointSize: CGFloat,
        monospaceFont: UIFont,
        colorScheme: ColorScheme,
        foreground originFG: UIColor,
        superscriptRaiseFactor: CGFloat,
        superscriptShrink: CGFloat
    ) {
        for run in content.runs {
            appendRun(
                run,
                into: out,
                state: state,
                basePointSize: basePointSize,
                monospaceFont: monospaceFont,
                colorScheme: colorScheme,
                originFG: originFG,
                superscriptRaiseFactor: superscriptRaiseFactor,
                superscriptShrink: superscriptShrink
            )
        }
    }

    private static func appendRun(
        _ run: InlineRun,
        into out: NSMutableAttributedString,
        state: InlineAttrState,
        basePointSize: CGFloat,
        monospaceFont: UIFont,
        colorScheme: ColorScheme,
        originFG: UIColor,
        superscriptRaiseFactor: CGFloat,
        superscriptShrink: CGFloat
    ) {
        let linkColor = MarkdownSelectableThemeUIKit.linkColor(for: colorScheme)

        func push(inner: InlineContent, next: InlineAttrState, fg: UIColor) {
            recurse(
                inner,
                into: out,
                state: next,
                basePointSize: basePointSize,
                monospaceFont: monospaceFont,
                colorScheme: colorScheme,
                foreground: fg,
                superscriptRaiseFactor: superscriptRaiseFactor,
                superscriptShrink: superscriptShrink
            )
        }

        func appendFragment(_ fragment: String, st: InlineAttrState, fg: UIColor, monoOverride: Bool) {
            guard !fragment.isEmpty else { return }

            let bodyFont =
                monoOverride
                ? monospace(at: monospaceFont.pointSize)
                : composedFont(pointSize: basePointSize, state: st, monospace: monospaceFont)

            var attrs: [NSAttributedString.Key: Any] = [
                .foregroundColor: fg,
                .font: bodyFont,
            ]
            let p = NSMutableParagraphStyle()
            p.lineSpacing = 0
            p.lineBreakMode = .byWordWrapping
            attrs[.paragraphStyle] = p

            if st.underline {
                attrs[.underlineStyle] = NSUnderlineStyle.single.rawValue
            }
            if st.strikethrough {
                attrs[.strikethroughStyle] = NSUnderlineStyle.single.rawValue
            }

            func scaled(_ f: UIFont, scale: CGFloat) -> UIFont {
                UIFont(descriptor: f.fontDescriptor, size: f.pointSize * scale)
            }

            if st.superscript {
                let small = scaled(bodyFont, scale: superscriptShrink)
                attrs[.font] = small
                attrs[.baselineOffset] = superscriptRaiseFactor * small.pointSize
            } else if st.subscripted {
                let small = scaled(bodyFont, scale: superscriptShrink)
                attrs[.font] = small
                attrs[.baselineOffset] = -(superscriptRaiseFactor * small.pointSize)
            }

            out.append(NSAttributedString(string: fragment, attributes: attrs))
        }

        var st = state
        switch run {
        case let .text(t):
            appendFragment(t, st: st, fg: originFG, monoOverride: false)
        case let .lineBreak(soft):
            appendFragment(soft ? " " : "\n", st: st, fg: originFG, monoOverride: false)
        case let .emphasis(inner):
            st.italic = true
            push(inner: inner, next: st, fg: originFG)
        case let .strong(inner):
            st.bold = true
            push(inner: inner, next: st, fg: originFG)
        case let .strikethrough(inner):
            st.strikethrough = true
            push(inner: inner, next: st, fg: originFG)
        case let .code(t):
            appendFragment(t, st: InlineAttrState(), fg: originFG, monoOverride: true)
        case let .highlight(inner):
            push(inner: inner, next: st, fg: originFG)
        case let .spoiler(inner):
            push(inner: inner, next: st, fg: MarkdownSelectableThemeUIKit.spoilerForeground())
        case let .link(inner, _, _):
            st.underline = true
            push(inner: inner, next: st, fg: linkColor)
        case let .autolink(_, _, display):
            var nl = InlineAttrState()
            nl.underline = true
            appendFragment(display, st: nl, fg: linkColor, monoOverride: false)
        case let .image(alt, _, _, _, _, _):
            appendFragment("[", st: st, fg: originFG, monoOverride: false)
            recurse(
                alt,
                into: out,
                state: st,
                basePointSize: basePointSize,
                monospaceFont: monospaceFont,
                colorScheme: colorScheme,
                foreground: originFG,
                superscriptRaiseFactor: superscriptRaiseFactor,
                superscriptShrink: superscriptShrink
            )
            appendFragment("]", st: st, fg: originFG, monoOverride: false)
        case let .footnoteRef(_, idx):
            let cap = captionFontSize(reference: basePointSize)
            let attr: [NSAttributedString.Key: Any] = [
                .font: cap,
                .foregroundColor: MarkdownSelectableThemeUIKit.secondaryLabel(),
            ]
            out.append(NSAttributedString(string: "[\(idx)]", attributes: attr))
        case let .mathInline(t):
            appendFragment(t, st: InlineAttrState(), fg: originFG, monoOverride: true)
        case let .emoji(_, uni):
            appendFragment(uni.map { String($0) } ?? "", st: st, fg: originFG, monoOverride: false)
        case let .styled(inner, _):
            push(inner: inner, next: st, fg: originFG)
        case let .abbreviation(short, _):
            appendFragment(short, st: st, fg: originFG, monoOverride: false)
        case let .kbd(t):
            appendFragment(t, st: InlineAttrState(), fg: originFG, monoOverride: true)
        case let .citation(key):
            appendFragment("[\(key)]", st: st, fg: originFG, monoOverride: false)
        case let .inserted(inner):
            st.underline = true
            push(inner: inner, next: st, fg: originFG)
        case let .superscript(inner):
            st.superscript = true
            push(inner: inner, next: st, fg: originFG)
        case let .subscripted(inner):
            st.subscripted = true
            push(inner: inner, next: st, fg: originFG)
        case let .wikiLink(_, label):
            var wl = st
            wl.underline = true
            appendFragment(label ?? "", st: wl, fg: linkColor, monoOverride: false)
        case let .ruby(base, ann):
            appendFragment("\(base) (\(ann))", st: st, fg: originFG, monoOverride: false)
        case let .htmlInline(h):
            appendFragment(h, st: InlineAttrState(), fg: MarkdownSelectableThemeUIKit.secondaryLabel(), monoOverride: true)
        case let .directiveInline(tag, _):
            appendFragment(
                ":\(tag):",
                st: InlineAttrState(),
                fg: MarkdownSelectableThemeUIKit.secondaryLabel(),
                monoOverride: true
            )
        }
    }
}

// MARK: - Font helpers

private func monospace(at size: CGFloat) -> UIFont {
    UIFont.monospacedSystemFont(ofSize: size, weight: .regular)
}

private func captionFontSize(reference _: CGFloat) -> UIFont {
    UIFont.preferredFont(forTextStyle: .caption1)
}

private func weightedSystemFont(size: CGFloat, weight: UIFont.Weight) -> UIFont {
    UIFont.systemFont(ofSize: size, weight: weight)
}

private func composedFont(pointSize base: CGFloat, state: InlineAttrState, monospace _: UIFont) -> UIFont {
    let anchor = UIFont.systemFont(ofSize: base, weight: .regular)
    var traits = anchor.fontDescriptor.symbolicTraits
    if state.bold { traits.insert(.traitBold) }
    if state.italic { traits.insert(.traitItalic) }
    guard traits != anchor.fontDescriptor.symbolicTraits,
          let merged = anchor.fontDescriptor.withSymbolicTraits(traits)
    else {
        return weightedSystemFont(
            size: base,
            weight: state.bold ? .semibold : .regular
        )
    }
    return UIFont(descriptor: merged, size: base)
}

#endif
