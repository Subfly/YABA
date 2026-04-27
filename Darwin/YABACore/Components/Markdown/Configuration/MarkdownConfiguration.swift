//
//  MarkdownConfiguration.swift
//  YABACore
//
//  Host-facing configuration for preview and editor.
//

import Foundation
import SwiftUI
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit) && !canImport(UIKit)
import AppKit
#endif

public struct MarkdownPreviewConfiguration: Sendable {
    public var showFrontMatter: Bool
    public var showLinkReferenceBlocks: Bool
    public var useWebViewForHtmlBlocks: Bool
    public var assetRegistry: MarkdownImageAssetRegistry
    public var baseURLForRelativeLinks: URL?

    public init(
        showFrontMatter: Bool = false,
        showLinkReferenceBlocks: Bool = false,
        useWebViewForHtmlBlocks: Bool = true,
        assetRegistry: MarkdownImageAssetRegistry = .init(),
        baseURLForRelativeLinks: URL? = nil
    ) {
        self.showFrontMatter = showFrontMatter
        self.showLinkReferenceBlocks = showLinkReferenceBlocks
        self.useWebViewForHtmlBlocks = useWebViewForHtmlBlocks
        self.assetRegistry = assetRegistry
        self.baseURLForRelativeLinks = baseURLForRelativeLinks
    }
}

public struct MarkdownEditorConfiguration: Sendable {
    #if canImport(UIKit)
    public var font: UIFont
    #elseif canImport(AppKit)
    public var font: NSFont
    #endif
    public var showInvisibleCharacters: Bool

    #if canImport(UIKit)
    public init(
        font: UIFont = .monospacedSystemFont(ofSize: 15, weight: .regular),
        showInvisibleCharacters: Bool = false
    ) {
        self.font = font
        self.showInvisibleCharacters = showInvisibleCharacters
    }
    #elseif canImport(AppKit)
    public init(
        font: NSFont = .monospacedSystemFont(ofSize: 15, weight: .regular),
        showInvisibleCharacters: Bool = false
    ) {
        self.font = font
        self.showInvisibleCharacters = showInvisibleCharacters
    }
    #endif
}
