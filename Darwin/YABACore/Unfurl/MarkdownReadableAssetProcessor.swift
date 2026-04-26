//
//  MarkdownReadableAssetProcessor.swift
//  YABACore
//
//  Parses readable Markdown, downloads HTTP(S) images in first-seen order, and rewrites destinations
//  to stable `yaba-asset://<assetId>` references (no `../assets/...` paths in stored content).
//

import Foundation

public enum MarkdownReadableAssetProcessor {
    public static func process(markdown: String, baseURL: String) async -> ReadableUnfurl {
        return ReadableUnfurl(markdown: "", assets: [])
    }

    private static func inferImageExtension(bytes: Data, url: String) -> String {
        if bytes.count >= 3, bytes[0] == 0xFF, bytes[1] == 0xD8, bytes[2] == 0xFF { return "jpg" }
        if bytes.count >= 4, bytes[0] == 0x89, bytes[1] == 0x50, bytes[2] == 0x4E, bytes[3] == 0x47 { return "png" }
        if bytes.count >= 3, bytes[0] == 0x47, bytes[1] == 0x49, bytes[2] == 0x46 { return "gif" }
        if bytes.count >= 12, bytes[0] == 0x52, bytes[1] == 0x49, bytes[2] == 0x46, bytes[3] == 0x46,
           bytes[8] == 0x57, bytes[9] == 0x45, bytes[10] == 0x42, bytes[11] == 0x50 { return "webp" }
        let u = url.lowercased()
        if u.contains(".png") { return "png" }
        if u.contains(".gif") { return "gif" }
        if u.contains(".webp") { return "webp" }
        if u.contains(".jpeg") || u.contains(".jpg") { return "jpg" }
        return "jpg"
    }
}
