//
//  YabaReadableModels.swift
//  YABACore
//
//  Parity with Compose `ReadableModels.kt` / processed converter output.
//

import Foundation

/// Result of readable content extraction, ready for SwiftData persistence.
public struct ReadableUnfurl: Sendable {
    /// GFM-oriented Markdown with `../assets/<id>.<ext>` image paths after asset download.
    public var markdown: String
    public var assets: [ReadableAssetPayload]

    public init(markdown: String, assets: [ReadableAssetPayload]) {
        self.markdown = markdown
        self.assets = assets
    }
}

/// One downloaded inline image for a readable version.
public struct ReadableAssetPayload: Sendable {
    public var assetId: String
    public var pathExtension: String
    public var bytes: Data

    public init(assetId: String, pathExtension: String, bytes: Data) {
        self.assetId = assetId
        self.pathExtension = pathExtension
        self.bytes = bytes
    }
}
