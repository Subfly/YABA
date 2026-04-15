//
//  BundleReader.swift
//  YABACore
//
//  Resolves bundled files by **resource name** only. Xcode copies resources into the app/framework
//  bundle without preserving project folder paths, so `Bundle.url(forResource:withExtension:subdirectory:)`
//  must use `subdirectory: nil` and the actual file name (e.g. `icon_categories_header.json`).
//

import Foundation

public enum BundleReaderError: Error, Sendable {
    case fileNotFound(String)
}

public enum BundleReader {
    // MARK: - URL resolution (flat bundle layout)

    /// Looks up a file in the bundle by **file name only** (e.g. `icon_categories_header.json`, `viewer.html`).
    public static func urlForBundledFileName(_ fileName: String, in bundle: Bundle = .main) -> URL? {
        let trimmed = fileName.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !trimmed.isEmpty else { return nil }
        let base = (trimmed as NSString).deletingPathExtension
        let ext = (trimmed as NSString).pathExtension
        return bundle.url(
            forResource: base,
            withExtension: ext.isEmpty ? nil : ext,
            subdirectory: nil
        )
    }

    /// Backward-compatible: accepts a path string but resolves using **only the last path component** (the file name).
    public static func url(forAssetPath assetPath: String, in bundle: Bundle = .main) -> URL? {
        let trimmed = assetPath.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !trimmed.isEmpty else { return nil }
        return urlForBundledFileName((trimmed as NSString).lastPathComponent, in: bundle)
    }

    /// Reads UTF-8 text from a bundled file (by path or file name; lookup uses the file name only).
    public static func readAssetText(_ assetPath: String, in bundle: Bundle = .main) throws -> String {
        guard let url = url(forAssetPath: assetPath, in: bundle) else {
            throw BundleReaderError.fileNotFound(assetPath)
        }
        return try String(contentsOf: url, encoding: .utf8)
    }

    // MARK: - Web components (HTML shells + chunks, all looked up by file name at bundle root)

    /// Directory URL suitable for `WKWebView.loadFileURL(_:allowingReadAccessTo:)` — parent of a known shell HTML file.
    public static func webComponentsBaseURL(in bundle: Bundle = .main) -> URL? {
        let entryNames = [
            "viewer.html",
            "editor.html",
            "canvas.html",
            "converter.html",
            "pdf-viewer.html",
            "epub-viewer.html",
        ]
        for name in entryNames {
            if let url = urlForBundledFileName(name, in: bundle) {
                return url.deletingLastPathComponent()
            }
        }
        return nil
    }

    public static func webComponentURL(named fileName: String, in bundle: Bundle = .main) -> URL? {
        urlForBundledFileName(fileName, in: bundle)
    }

    public static func getViewerURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "viewer.html", in: bundle)
    }

    public static func getEditorURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "editor.html", in: bundle)
    }

    public static func getCanvasURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "canvas.html", in: bundle)
    }

    public static func getConverterURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "converter.html", in: bundle)
    }

    public static func getPdfViewerURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "pdf-viewer.html", in: bundle)
    }

    public static func getEpubViewerURL(in bundle: Bundle = .main) -> URL? {
        webComponentURL(named: "epub-viewer.html", in: bundle)
    }
}
