//
//  MarkdownFencedCodeBlockView.swift
//  YABACore
//

import SwiftUI

/// Fenced code with optional line numbers. Gutter stays fixed while only the code scrolls horizontally.
struct MarkdownFencedCodeBlockView: View {
    let model: FencedCodeBlockModel
    let theme: MarkdownThemeTokens

    /// Space below the last code line inside the horizontal `ScrollView` so the system scroll indicator doesn’t cover descenders.
    private static let horizontalScrollContentBottomInset: CGFloat = 10

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                if !model.info.isEmpty {
                    MarkdownSelectablePlainText(
                        verbatim: model.info,
                        semantic: .caption1,
                        weight: .regular,
                        monospaced: false,
                        foreground: .secondary
                    )
                }
                if let t = model.title, !t.isEmpty {
                    MarkdownSelectablePlainText(
                        verbatim: t,
                        semantic: .caption1,
                        weight: .regular,
                        monospaced: false
                    )
                }
            }
            if model.showLineNumbers {
                lineNumberedBody
            } else {
                ScrollView(.horizontal, showsIndicators: true) {
                    MarkdownSelectablePlainText(
                        verbatim: displayCode,
                        semantic: .body,
                        weight: .regular,
                        monospaced: true
                    )
                    .padding(.bottom, Self.horizontalScrollContentBottomInset)
                }
                .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(theme.codeBackground, in: RoundedRectangle(cornerRadius: 6))
    }

    private var displayCode: String {
        MarkdownCodeBlockLiteralNormalization.forDisplay(model.code)
    }

    private var codeLines: [String] {
        let lines = displayCode.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        return lines.isEmpty ? [""] : lines
    }

    private var lineNumberedBody: some View {
        let lines = codeLines
        let start = model.startLine
        let lastIndex = max(0, lines.count - 1)
        let gutterChars = String(start + lastIndex).count
        let lastLineBottomInset = Self.horizontalScrollContentBottomInset

        return HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .trailing, spacing: 0) {
                ForEach(Array(lines.enumerated()), id: \.offset) { i, _ in
                    MarkdownSelectablePlainText(
                        verbatim: paddedLineNumber(start + i, width: gutterChars),
                        semantic: .body,
                        weight: .regular,
                        monospaced: true,
                        foreground: .secondary
                    )
                    .padding(.bottom, i == lastIndex ? lastLineBottomInset : 0)
                }
            }
            .fixedSize(horizontal: true, vertical: false)

            ScrollView(.horizontal, showsIndicators: true) {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(lines.enumerated()), id: \.offset) { i, line in
                        MarkdownSelectablePlainText(
                            verbatim: line.isEmpty ? "\u{00a0}" : line,
                            semantic: .body,
                            weight: .regular,
                            monospaced: true
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, i == lastIndex ? lastLineBottomInset : 0)
                    }
                }
            }
            .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        }
    }

    private func paddedLineNumber(_ n: Int, width: Int) -> String {
        let s = String(n)
        if s.count >= width { return s }
        return String(repeating: " ", count: width - s.count) + s
    }
}
