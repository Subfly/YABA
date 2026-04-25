//
//  ConverterResultProcessor.swift
//  YABACore
//
//  Parity with Compose `ConverterResultProcessor.kt`.
//

import Foundation

public enum ConverterResultProcessor {
    /// Downloads remote assets, rewrites `yaba-asset://` placeholders in document JSON to `../assets/<id>.<ext>`.
    public static func process(
        documentJson: String,
        assets: [WebConverterAsset]
    ) async -> ReadableUnfurl {
        var readables: [(ReadableAssetPayload, String, String)] = []
        for asset in assets {
            guard let url = URL(string: asset.url),
                  url.scheme == "http" || url.scheme == "https"
            else { continue }
            guard let bytes = try? await UnfurlHttpClient.getBytes(url: url) else { continue }
            let outBytes = YabaImageCompression.compressDataPreservingFormat(bytes)
            let ext = inferImageExtension(bytes: outBytes, url: asset.url)
            let assetId = UUID().uuidString
            let relativePath = "../assets/\(assetId).\(ext)"
            let payload = ReadableAssetPayload(assetId: assetId, pathExtension: ext, bytes: outBytes)
            readables.append((payload, asset.placeholder, relativePath))
        }
        var resultBody = documentJson
        for (_, placeholder, replacement) in readables {
            resultBody = resultBody.replacingOccurrences(of: placeholder, with: replacement)
        }
        return ReadableUnfurl(
            html: resultBody,
            assets: readables.map { $0.0 }
        )
    }

    private static func inferImageExtension(bytes: Data, url: String) -> String {
        if bytes.count >= 3,
           bytes[0] == 0xFF, bytes[1] == 0xD8, bytes[2] == 0xFF {
            return "jpg"
        }
        if bytes.count >= 4,
           bytes[0] == 0x89, bytes[1] == 0x50, bytes[2] == 0x4E, bytes[3] == 0x47 {
            return "png"
        }
        if bytes.count >= 3,
           bytes[0] == 0x47, bytes[1] == 0x49, bytes[2] == 0x46 {
            return "gif"
        }
        if bytes.count >= 12,
           bytes[0] == 0x52, bytes[1] == 0x49, bytes[2] == 0x46, bytes[3] == 0x46,
           bytes[8] == 0x57, bytes[9] == 0x45, bytes[10] == 0x42, bytes[11] == 0x50 {
            return "webp"
        }
        let urlLower = url.lowercased()
        if urlLower.contains(".png") { return "png" }
        if urlLower.contains(".gif") { return "gif" }
        if urlLower.contains(".webp") { return "webp" }
        if urlLower.contains(".jpeg") || urlLower.contains(".jpg") { return "jpg" }
        return "jpg"
    }
}
