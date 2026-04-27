//
//  MarkdownImageAssetRegistry.swift
//  YABACore
//
//  Resolves `yaba-asset://` URLs to image bytes (`Data`) supplied by the host (e.g. from SwiftData).
//

import Foundation

/// Resolves `yaba-asset://` and similar using `ReadableAssetPayload` bytes from the host.
public struct MarkdownImageAssetRegistry: Sendable {
    public var assetsById: [String: ReadableAssetPayload]

    public init(assetsById: [String: ReadableAssetPayload] = [:]) {
        self.assetsById = assetsById
    }

    public func imageData(forAssetURL url: URL) -> Data? {
        guard url.scheme == "yaba-asset" else { return nil }
        let id: String?
        if let host = url.host, !host.isEmpty {
            id = host
        } else {
            let part = url.pathComponents.dropFirst().first
            id = part.map { String($0) }
        }
        guard let id else { return nil }
        return assetsById[id]?.bytes
    }
}
