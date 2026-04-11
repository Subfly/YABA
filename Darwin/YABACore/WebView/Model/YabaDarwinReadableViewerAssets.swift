//
//  YabaDarwinReadableViewerAssets.swift
//  YABACore
//
//  Use with `YabaDarwinWebFeature.readableViewer` / editor `setDocumentJson` `assetsBaseUrl` so
//  `../assets/<id>.<ext>` in stored JSON resolves via [YabaDarwinReadableAssetSchemeHandler].
//

import Foundation

public enum YabaDarwinReadableViewerAssets {
    /// Pass as `assetsBaseUrl` when loading reader JSON that uses `../assets/...` paths from [DarwinConverterResultProcessor].
    public static let assetsBaseURLForYabaAssetScheme = "yaba-asset://resolved"
}
