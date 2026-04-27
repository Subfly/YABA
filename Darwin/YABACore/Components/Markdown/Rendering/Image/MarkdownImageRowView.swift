//
//  MarkdownImageRowView.swift
//  YABACore
//
//  Block-level images: `yaba-asset://` bytes from the host registry (SwiftUI `Image` from
//  UIKit/AppKit), or `http`/`https`/`file` URLs via Kingfisher.
//

import Kingfisher
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

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.markdownTheme) private var themeOverride

    var body: some View {
        let theme = themeOverride ?? MarkdownThemeTokens.standard(colorScheme: colorScheme)
        VStack(alignment: .leading, spacing: 4) {
            if let url = resolveURL() {
                if url.scheme == "yaba-asset" {
                    MarkdownYabaAssetImageView(
                        url: url,
                        alt: alt,
                        width: width,
                        height: height,
                        registry: registry,
                        theme: theme
                    )
                } else if ["http", "https", "file"].contains(url.scheme?.lowercased()) {
                    remoteKingfisherImage(source: url.convertToSource(), theme: theme)
                } else {
                    imageFailurePlaceholder(theme: theme)
                }
            } else {
                imageFailurePlaceholder(theme: theme)
            }
        }
    }

    // MARK: - Remote (Kingfisher)

    private func remoteKingfisherImage(source: Source, theme: MarkdownThemeTokens) -> some View {
        let maxW: CGFloat = width.map { min(CGFloat($0), 600) } ?? 600
        let maxH: CGFloat? = height.map { CGFloat(min($0, 1200)) }
        let scale = markdownImageDisplayScale()
        let downsample = CGSize(
            width: maxW * scale,
            height: (maxH ?? maxW) * scale
        )
        return KFImage.source(source)
            .placeholder { _ in
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(theme.codeBackground)
                    ProgressView()
                }
                .frame(height: 120)
            }
            .onFailureView {
                imageFailurePlaceholder(theme: theme)
            }
            .cancelOnDisappear(true)
            .downsampling(size: downsample)
            .resizable()
            .scaledToFit()
            .frame(maxWidth: maxW, maxHeight: maxH)
    }

    @ViewBuilder
    private func imageFailurePlaceholder(theme: MarkdownThemeTokens) -> some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(theme.codeBackground)
            .frame(height: 64)
            .overlay {
                MarkdownCombinedInlineText(content: alt, theme: theme) { _ in }
            }
    }

    private func resolveURL() -> URL? {
        if let u = URL(string: urlString), u.scheme != nil { return u }
        return nil
    }

    private func markdownImageDisplayScale() -> CGFloat {
        #if canImport(UIKit)
        return UIScreen.main.scale
        #elseif canImport(AppKit)
        return NSScreen.main?.backingScaleFactor ?? 2
        #else
        return 2
        #endif
    }
}

// MARK: - yaba-asset (Data from registry → native image)

private struct MarkdownYabaAssetImageView: View {
    let url: URL
    let alt: InlineContent
    var width: Int?
    var height: Int?
    var registry: MarkdownImageAssetRegistry
    let theme: MarkdownThemeTokens

    #if canImport(UIKit)
    @State private var uiImage: UIImage?
    @State private var loadFailed = false
    #elseif canImport(AppKit)
    @State private var nsImage: NSImage?
    @State private var loadFailed = false
    #endif

    var body: some View {
        let maxW: CGFloat = width.map { min(CGFloat($0), 600) } ?? 600
        let maxH: CGFloat? = height.map { CGFloat(min($0, 1200)) }
        #if canImport(UIKit)
        Group {
            if let img = uiImage {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: maxW, maxHeight: maxH)
            } else if loadFailed {
                yabaFailure
            } else {
                yabaLoading
            }
        }
        .task(id: url.absoluteString) {
            await loadYabaAssetUIKit()
        }
        #elseif canImport(AppKit)
        Group {
            if let img = nsImage {
                Image(nsImage: img)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: maxW, maxHeight: maxH)
            } else if loadFailed {
                yabaFailure
            } else {
                yabaLoading
            }
        }
        .task(id: url.absoluteString) {
            await loadYabaAssetAppKit()
        }
        #else
        yabaFailure
        #endif
    }

    private var yabaLoading: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(theme.codeBackground)
            ProgressView()
        }
        .frame(height: 120)
    }

    private var yabaFailure: some View {
        RoundedRectangle(cornerRadius: 4)
            .fill(theme.codeBackground)
            .frame(height: 64)
            .overlay {
                MarkdownCombinedInlineText(content: alt, theme: theme) { _ in }
            }
    }

    #if canImport(UIKit)
    private func loadYabaAssetUIKit() async {
        let data = registry.imageData(forAssetURL: url)
        let img: UIImage? = await Task.detached(priority: .userInitiated) {
            guard let data else { return nil }
            return UIImage(data: data)
        }.value
        await MainActor.run {
            if let img {
                uiImage = img
                loadFailed = false
            } else {
                uiImage = nil
                loadFailed = true
            }
        }
    }
    #elseif canImport(AppKit)
    private func loadYabaAssetAppKit() async {
        let data = registry.imageData(forAssetURL: url)
        let img: NSImage? = await Task.detached(priority: .userInitiated) {
            guard let data else { return nil }
            return NSImage(data: data)
        }.value
        await MainActor.run {
            if let img {
                nsImage = img
                loadFailed = false
            } else {
                nsImage = nil
                loadFailed = true
            }
        }
    }
    #endif
}
