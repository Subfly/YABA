//
//  YabaReadableModels.swift
//  YABACore
//
//  Parity with Compose `ReadableModels.kt` / processed converter output.
//

import Foundation

/// Result of readable content extraction, ready for SwiftData persistence.
public struct ReadableUnfurl: Sendable {
    /// Rich-text document JSON with `../assets/<id>.<ext>` paths (Compose parity).
    public var documentJson: String
    public var assets: [ReadableAssetPayload]

    public init(documentJson: String, assets: [ReadableAssetPayload]) {
        self.documentJson = documentJson
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
