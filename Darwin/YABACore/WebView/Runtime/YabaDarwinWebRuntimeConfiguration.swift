//
//  YabaDarwinWebRuntimeConfiguration.swift
//  YABACore
//

import WebKit

/// Tunable knobs for `YabaWKWebViewRuntime` (security-first defaults).
public struct YabaDarwinWebRuntimeConfiguration: @unchecked Sendable {
    /// When false, navigation delegate cancels non-file remote HTTP(S) loads.
    public var allowsRemoteHTTP: Bool

    /// Default off; enable only for shells that must load remote PDF/EPUB URLs.
    public var allowsRemoteNavigation: Bool

    public var websiteDataStore: WKWebsiteDataStore

    public init(
        allowsRemoteHTTP: Bool = false,
        allowsRemoteNavigation: Bool = false,
        websiteDataStore: WKWebsiteDataStore = .nonPersistent()
    ) {
        self.allowsRemoteHTTP = allowsRemoteHTTP
        self.allowsRemoteNavigation = allowsRemoteNavigation
        self.websiteDataStore = websiteDataStore
    }
}
