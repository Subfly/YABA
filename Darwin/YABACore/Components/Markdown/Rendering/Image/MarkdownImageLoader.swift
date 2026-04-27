//
//  MarkdownImageLoader.swift
//  YABACore
//
//  Async image loading for remote, file, and yaba-asset URLs with memory cache.
//  iOS / vision / tv / Catalyst: UIKit. Native macOS: AppKit.
//

import CoreGraphics
import Foundation
import ImageIO

#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif

// MARK: - Shared decode (Image I/O; avoids deprecated / Swift-unexposed C helpers)

private enum MarkdownImageIO {
    private static let maxThumbnailPixel: Int = 800

    /// Thumbnail at most `maxThumbnailPixel` on the long edge; safe off the main thread.
    static func cgImageThumbnail(from data: Data) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceThumbnailMaxPixelSize: maxThumbnailPixel,
        ]
        return CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
    }
}

// MARK: - Public types

public struct MarkdownImageLoadRequest: Sendable, Hashable {
    public var url: URL
    public init(url: URL) { self.url = url }
}

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

#if canImport(UIKit)

/// On Apple platforms with UIKit (iOS, iPadOS, visionOS, tvOS, Mac Catalyst, etc.).
public typealias MarkdownPlatformImage = UIImage

public final class MarkdownImageCache: @unchecked Sendable {
    public static let shared = MarkdownImageCache()

    private let mem = NSCache<NSString, UIImage>()
    private let session: URLSession

    public init() {
        let c = URLSessionConfiguration.ephemeral
        c.requestCachePolicy = .returnCacheDataElseLoad
        c.urlCache = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 0,
            directory: nil
        )
        session = URLSession(configuration: c)
        mem.countLimit = 200
    }

    public func cachedImage(for request: MarkdownImageLoadRequest) -> UIImage? {
        mem.object(forKey: request.url.absoluteString as NSString)
    }

    public func image(
        for request: MarkdownImageLoadRequest,
        registry: MarkdownImageAssetRegistry = .init()
    ) async -> UIImage? {
        let key = request.url.absoluteString as NSString
        if let u = mem.object(forKey: key) { return u }

        if request.url.scheme == "yaba-asset", let data = registry.imageData(forAssetURL: request.url) {
            let img = await decodeOffMainThread(data: data)
            if let img { mem.setObject(img, forKey: key) }
            return img
        }

        if request.url.isFileURL {
            let data = try? Data(contentsOf: request.url)
            let img = data.flatMap { Self.uiImage(from: $0) }
            if let img { mem.setObject(img, forKey: key) }
            return img
        }

        guard request.url.scheme == "http" || request.url.scheme == "https" else { return nil }

        return await withCheckedContinuation { cont in
            let t = session.dataTask(with: request.url) { [weak self] data, _, _ in
                guard let self, let data else {
                    cont.resume(returning: nil)
                    return
                }
                let img = Self.uiImage(from: data)
                if let img { self.mem.setObject(img, forKey: key) }
                cont.resume(returning: img)
            }
            t.resume()
        }
    }

    private func decodeOffMainThread(data: Data) async -> UIImage? {
        await withCheckedContinuation { cont in
            DispatchQueue.global(qos: .userInitiated).async {
                cont.resume(returning: Self.uiImage(from: data))
            }
        }
    }

    private static func displayScaleForDecode() -> CGFloat {
        2.0
    }

    private static func uiImage(from data: Data) -> UIImage? {
        if let cg = MarkdownImageIO.cgImageThumbnail(from: data) {
            return UIImage(
                cgImage: cg,
                scale: displayScaleForDecode(),
                orientation: .up
            )
        }
        return UIImage(data: data)
    }
}

#elseif canImport(AppKit)

/// On native macOS (AppKit); YABAShareMac and similar.
public typealias MarkdownPlatformImage = NSImage

public final class MarkdownImageCache: @unchecked Sendable {
    public static let shared = MarkdownImageCache()

    private let mem = NSCache<NSString, NSImage>()
    private let session: URLSession

    public init() {
        let c = URLSessionConfiguration.ephemeral
        c.requestCachePolicy = .returnCacheDataElseLoad
        c.urlCache = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 0,
            directory: nil
        )
        session = URLSession(configuration: c)
        mem.countLimit = 200
    }

    public func cachedImage(for request: MarkdownImageLoadRequest) -> NSImage? {
        mem.object(forKey: request.url.absoluteString as NSString)
    }

    public func image(
        for request: MarkdownImageLoadRequest,
        registry: MarkdownImageAssetRegistry = .init()
    ) async -> NSImage? {
        let key = request.url.absoluteString as NSString
        if let u = mem.object(forKey: key) { return u }

        if request.url.scheme == "yaba-asset", let data = registry.imageData(forAssetURL: request.url) {
            let img = await decodeOffMainThread(data: data)
            if let img { mem.setObject(img, forKey: key) }
            return img
        }

        if request.url.isFileURL {
            let data = try? Data(contentsOf: request.url)
            let img = data.flatMap { Self.nsImage(from: $0) }
            if let img { mem.setObject(img, forKey: key) }
            return img
        }

        guard request.url.scheme == "http" || request.url.scheme == "https" else { return nil }

        return await withCheckedContinuation { cont in
            let t = session.dataTask(with: request.url) { [weak self] data, _, _ in
                guard let self, let data else {
                    cont.resume(returning: nil)
                    return
                }
                let img = Self.nsImage(from: data)
                if let img { self.mem.setObject(img, forKey: key) }
                cont.resume(returning: img)
            }
            t.resume()
        }
    }

    private func decodeOffMainThread(data: Data) async -> NSImage? {
        await withCheckedContinuation { cont in
            DispatchQueue.global(qos: .userInitiated).async {
                cont.resume(returning: Self.nsImage(from: data))
            }
        }
    }

    private static func nsImage(from data: Data) -> NSImage? {
        if let cg = MarkdownImageIO.cgImageThumbnail(from: data) {
            return NSImage(
                cgImage: cg,
                size: NSSize(
                    width: CGFloat(max(cg.width, 1)),
                    height: CGFloat(max(cg.height, 1))
                )
            )
        }
        return NSImage(data: data)
    }
}

#endif
