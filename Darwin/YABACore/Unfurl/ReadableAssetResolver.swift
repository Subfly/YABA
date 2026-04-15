//
//  ReadableAssetResolver.swift
//  YABACore
//
//  In-memory registry of readable inline image bytes keyed by asset id, used by
//  [ReadableAssetSchemeHandler] when the WebView resolves `yaba-asset:` image URLs.
//

import Foundation

public final class ReadableAssetResolver: @unchecked Sendable {
    public static let shared = ReadableAssetResolver()

    private let lock = NSLock()
    private var bytesByAssetId: [String: Data] = [:]

    private init() {}

    public func register(assetId: String, bytes: Data) {
        lock.lock()
        defer { lock.unlock() }
        bytesByAssetId[assetId] = bytes
    }

    public func register(unfurl: ReadableUnfurl) {
        lock.lock()
        defer { lock.unlock() }
        for a in unfurl.assets {
            bytesByAssetId[a.assetId] = a.bytes
        }
    }

    public func bytes(forAssetId assetId: String) -> Data? {
        lock.lock()
        defer { lock.unlock() }
        return bytesByAssetId[assetId]
    }

    public func clear() {
        lock.lock()
        defer { lock.unlock() }
        bytesByAssetId.removeAll()
    }
}
