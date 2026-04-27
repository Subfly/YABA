//
//  MarkdownSyntaxHighlighter.swift
//  YABACore
//
//  Lightweight regex-based Markdown styling for `UITextView` (TextKit 2).
//

import Foundation
import UIKit

enum MarkdownSyntaxHighlighter {
    /// Applies attributes to `text` without changing substrings (safe for selection round-trips).
    static func attributedMarkdown(
        text: String,
        baseFont: UIFont,
        showInvisibles: Bool
    ) -> NSAttributedString {
        let m = NSMutableAttributedString(
            string: text,
            attributes: [
                .font: baseFont,
                .foregroundColor: UIColor.label,
            ]
        )

        let heading = try? NSRegularExpression(pattern: "(?m)^#{1,6}\\s+.+$", options: [])
        let fenceOpen = try? NSRegularExpression(pattern: "(?m)^```.*$", options: [])
        let inlineCode = try? NSRegularExpression(pattern: "`[^`]+`", options: [])
        let link = try? NSRegularExpression(pattern: "\\[[^\\]]+\\]\\([^)]+\\)", options: [])
        let bold = try? NSRegularExpression(pattern: "\\*\\*[^*]+\\*\\*|__[^_]+__", options: [])
        let italic = try? NSRegularExpression(pattern: "(?<!\\*)\\*[^*]+\\*(?!\\*)|_[^_]+_", options: [])
        let hr = try? NSRegularExpression(pattern: "(?m)^(?:\\*{3,}|-{3,}|_{3,})\\s*$", options: [])
        let list = try? NSRegularExpression(pattern: "(?m)^\\s*(?:[-*+]|[0-9]+\\.)\\s+\\S", options: [])

        let full = NSRange(text.startIndex..., in: text)
        let mono = UIFont.monospacedSystemFont(ofSize: baseFont.pointSize, weight: .regular)
        let big = UIFont.systemFont(ofSize: baseFont.pointSize + 2, weight: .semibold)

        func apply(_ re: NSRegularExpression?, color: UIColor, font: UIFont? = nil) {
            guard let re else { return }
            re.enumerateMatches(in: text, options: [], range: full) { result, _, _ in
                guard let r = result?.range else { return }
                m.addAttributes([
                    .foregroundColor: color,
                    .font: font ?? baseFont,
                ], range: r)
            }
        }

        apply(heading, color: .secondaryLabel, font: big)
        apply(fenceOpen, color: .systemOrange, font: mono)
        apply(inlineCode, color: .label, font: mono)
        apply(link, color: .systemBlue)
        apply(bold, color: .label, font: .boldSystemFont(ofSize: baseFont.pointSize))
        apply(italic, color: .label, font: UIFont(
            descriptor: baseFont.fontDescriptor.withSymbolicTraits(.traitItalic) ?? baseFont.fontDescriptor,
            size: baseFont.pointSize
        ))
        apply(hr, color: .tertiaryLabel)
        apply(list, color: .secondaryLabel)

        if showInvisibles {
            // optional: mark trailing spaces — skipped in first pass
        }

        return m
    }
}
