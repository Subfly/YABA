//
//  ReadableViewerAssets.swift
//  YABACore
//
//  Use with `WebFeature.readableViewer` / editor `setDocumentJson` `assetsBaseUrl` so
//  `../assets/<id>.<ext>` in stored JSON resolves via [ReadableAssetSchemeHandler].
//

import Foundation

public enum ReadableViewerAssets {
    /// Pass as `assetsBaseUrl` when loading reader JSON that uses `../assets/...` paths from [ConverterResultProcessor].
    public static let assetsBaseURLForYabaAssetScheme = "yaba-asset://resolved"
}
