//
//  BundleReader.swift
//  YABACore
//
//  Reads bundled resources under `Darwin/YABACore/Assets` (metadata, web-components) from the app
//  bundle. Mirrors the role of Compose `dev.subfly.yaba.core.util.BundleReader`.
//

import Foundation

public enum BundleReaderError: Error, Sendable {
    case fileNotFound(String)
}

/// Resolves and reads files synced under `YABACore/Assets` (e.g. `Assets/Metadata/...`, `Assets/WebComponents/...`).
public enum BundleReader {
    /// Directory segment for yaba-web-components HTML and chunks (matches `Scripts/build_web_components.sh` output).
    public static let webComponentsAssetDirectory = "Assets/WebComponents"

    /// Resolves a bundle resource URL from a `/`-separated path relative to the module bundle root, e.g. `Assets/Metadata/icon_categories_header.json`.
    public static func url(forAssetPath assetPath: String, in bundle: Bundle = .main) -> URL? {
        let path = assetPath.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard !path.isEmpty else { return nil }
        let nsPath = path as NSString
        let directory = nsPath.deletingLastPathComponent
        let fileName = nsPath.lastPathComponent
        let base = (fileName as NSString).deletingPathExtension
        let ext = (fileName as NSString).pathExtension
        let subdirectory = directory.isEmpty ? nil : directory
        return bundle.url(
            forResource: base,
            withExtension: ext.isEmpty ? nil : ext,
            subdirectory: subdirectory
        )
    }

    /// Reads UTF-8 text from a bundled asset path.
    public static func readAssetText(_ assetPath: String, in bundle: Bundle = .main) throws -> String {
        guard let url = url(forAssetPath: assetPath, in: bundle) else {
            throw BundleReaderError.fileNotFound(assetPath)
        }
        return try String(contentsOf: url, encoding: .utf8)
    }

    /// Base directory URL for web-component assets (chunks, CSS), for `WKWebView` / `loadFileURL` base URLs.
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
            if let url = url(forAssetPath: "\(webComponentsAssetDirectory)/\(name)", in: bundle) {
                return url.deletingLastPathComponent()
            }
        }
        return nil
    }

    public static func webComponentURL(named fileName: String, in bundle: Bundle = .main) -> URL? {
        url(forAssetPath: "\(webComponentsAssetDirectory)/\(fileName)", in: bundle)
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
