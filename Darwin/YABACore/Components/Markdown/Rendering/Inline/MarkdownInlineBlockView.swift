//
//  MarkdownInlineBlockView.swift
//  YABACore
//
//  Main inline stack: text runs and block-level image rows.
//

import SwiftUI

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
