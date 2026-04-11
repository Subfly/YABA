//
//  ReadableAssetSchemeHandler.swift
//  YABACore
//
//  Serves bytes registered in [ReadableAssetResolver] for `yaba-asset:` image URLs
//  (avoids relying on filesystem paths while keeping SwiftData-backed `Data`).
//

import Foundation
import WebKit

public final class ReadableAssetSchemeHandler: NSObject, WKURLSchemeHandler {
    public override init() {
        super.init()
    }

    public func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(NSError(domain: "YabaAsset", code: 400, userInfo: [NSLocalizedDescriptionKey: "Bad URL"]))
            return
        }
        let last = url.lastPathComponent
        let assetId = (last as NSString).deletingPathExtension
        let ext = (last as NSString).pathExtension.lowercased()
        guard !assetId.isEmpty, let data = ReadableAssetResolver.shared.bytes(forAssetId: assetId) else {
            urlSchemeTask.didFailWithError(NSError(domain: "YabaAsset", code: 404, userInfo: [NSLocalizedDescriptionKey: "Asset not found"]))
            return
        }
        let mime = Self.mimeType(forExtension: ext)
        let response = URLResponse(
            url: url,
            mimeType: mime,
            expectedContentLength: data.count,
            textEncodingName: nil
        )
        urlSchemeTask.didReceive(response)
        urlSchemeTask.didReceive(data)
        urlSchemeTask.didFinish()
    }

    public func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {}

    private static func mimeType(forExtension ext: String) -> String {
        switch ext {
        case "png": return "image/png"
        case "gif": return "image/gif"
        case "webp": return "image/webp"
        case "jpg", "jpeg": return "image/jpeg"
        default: return "application/octet-stream"
        }
    }
}
