//
//  ReadableViewerAssets.swift
//  YABACore
//
//  Use with `WebFeature.readableViewer` / editor `setDocumentJson` `assetsBaseUrl` so
//  `../assets/<id>.<ext>` in stored JSON resolves via [ReadableAssetSchemeHandler].
//

import Foundation

public enum ReadableViewerAssets {
    /// Pass as `assetsBaseUrl` when loading reader content that references `../assets/...` or `yaba-asset:` URLs.
    public static let assetsBaseURLForYabaAssetScheme = "yaba-asset://resolved"
}
