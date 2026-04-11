//
//  YabaReadableViewerAssets.swift
//  YABACore
//
//  Use with `YabaWebFeature.readableViewer` / editor `setDocumentJson` `assetsBaseUrl` so
//  `../assets/<id>.<ext>` in stored JSON resolves via [YabaReadableAssetSchemeHandler].
//

import Foundation

public enum YabaReadableViewerAssets {
    /// Pass as `assetsBaseUrl` when loading reader JSON that uses `../assets/...` paths from [DarwinConverterResultProcessor].
    public static let assetsBaseURLForYabaAssetScheme = "yaba-asset://resolved"
}
