//
//  MarkdownImageRowView.swift
//  YABACore
//
//  Block-level image row: `AsyncImage` for http(s), asset/cache path for other URLs.
//

import SwiftUI
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif

struct MarkdownImageRowView: View {
    let urlString: String
    let alt: InlineContent
    var width: Int?
    var height: Int?
    var registry: MarkdownImageAssetRegistry

    #if canImport(UIKit)
    @State private var loadedUIKit: UIImage?
    #elseif canImport(AppKit)
    @State private var loadedAppKit: NSImage?
    #endif
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.markdownTheme) private var themeOverride

    var body: some View {
        let theme = themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)
        VStack(alignment: .leading, spacing: 4) {
            Group {
                if let u = resolveURL() {
                    if isRemoteHttpURL(u) {
                        asyncRemoteImage(url: u, theme: theme)
                    } else {
                        localOrAssetImage(url: u, theme: theme)
                    }
                } else {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(theme.codeBackground)
                        .frame(height: 64)
                        .overlay {
                            MarkdownCombinedInlineText(
                                content: alt,
                                theme: theme
                            ) { _ in }
                        }
                }
            }
        }
    }

    private func isRemoteHttpURL(_ u: URL) -> Bool {
        u.scheme == "http" || u.scheme == "https"
    }

    @ViewBuilder
    private func asyncRemoteImage(url: URL, theme: MarkdownThemeTokens) -> some View {
        let maxW: CGFloat? = width.map { min(CGFloat($0), 600) }
        AsyncImage(url: url) { phase in
            switch phase {
            case .empty:
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(theme.codeBackground)
                    ProgressView()
                }
                .frame(height: 120)
            case .success(let image):
                image
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: maxW ?? 600)
            case .failure:
                RoundedRectangle(cornerRadius: 4)
                    .fill(theme.codeBackground)
                    .frame(height: 64)
                    .overlay {
                        MarkdownCombinedInlineText(content: alt, theme: theme) { _ in }
                    }
            @unknown default:
                ProgressView()
            }
        }
    }

    @ViewBuilder
    private func localOrAssetImage(url: URL, theme: MarkdownThemeTokens) -> some View {
        #if canImport(UIKit)
        if let loaded = loadedUIKit {
            imageViewUIKit(loaded)
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(theme.codeBackground)
                ProgressView()
            }
            .frame(height: 120)
            .task(id: url.absoluteString) {
                let img = await MarkdownImageCache.shared.image(
                    for: MarkdownImageLoadRequest(url: url),
                    registry: registry
                )
                await MainActor.run { loadedUIKit = img }
            }
        }
        #elseif canImport(AppKit)
        if let loaded = loadedAppKit {
            imageViewAppKit(loaded)
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(theme.codeBackground)
                ProgressView()
            }
            .frame(height: 120)
            .task(id: url.absoluteString) {
                let img = await MarkdownImageCache.shared.image(
                    for: MarkdownImageLoadRequest(url: url),
                    registry: registry
                )
                await MainActor.run { loadedAppKit = img }
            }
        }
        #endif
    }

    #if canImport(UIKit)
    @ViewBuilder
    private func imageViewUIKit(_ uiImage: UIImage) -> some View {
        let w = CGFloat(width.map { min(CGFloat($0), 600) } ?? min(uiImage.size.width, 600))
        Image(uiImage: uiImage)
            .resizable()
            .scaledToFit()
            .frame(maxWidth: w)
    }
    #endif

    #if canImport(AppKit) && !canImport(UIKit)
    @ViewBuilder
    private func imageViewAppKit(_ nsImage: NSImage) -> some View {
        let w = CGFloat(width.map { min(CGFloat($0), 600) } ?? min(nsImage.size.width, 600))
        Image(nsImage: nsImage)
            .resizable()
            .scaledToFit()
            .frame(maxWidth: w)
    }
    #endif

    private func resolveURL() -> URL? {
        if let u = URL(string: urlString), u.scheme != nil { return u }
        return nil
    }
}
