//
//  WebRuntimeConfiguration.swift
//  YABACore
//

import WebKit

/// Tunable knobs for `WKWebViewRuntime` (security-first defaults).
public struct WebRuntimeConfiguration: @unchecked Sendable {
    /// When false, navigation delegate cancels non-file remote HTTP(S) loads.
    public var allowsRemoteHTTP: Bool

    /// Default off; enable only for shells that must load remote PDF/EPUB URLs.
    public var allowsRemoteNavigation: Bool

    public var websiteDataStore: WKWebsiteDataStore

    /// When set, registers a `WKURLSchemeHandler` for the `yaba-asset` scheme (inline reader images from SwiftData).
    public var yabaAssetSchemeHandler: (WKURLSchemeHandler & NSObject)?

    public init(
        allowsRemoteHTTP: Bool = false,
        allowsRemoteNavigation: Bool = false,
        websiteDataStore: WKWebsiteDataStore = .nonPersistent(),
        yabaAssetSchemeHandler: (WKURLSchemeHandler & NSObject)? = nil
    ) {
        self.allowsRemoteHTTP = allowsRemoteHTTP
        self.allowsRemoteNavigation = allowsRemoteNavigation
        self.websiteDataStore = websiteDataStore
        self.yabaAssetSchemeHandler = yabaAssetSchemeHandler
    }
}
