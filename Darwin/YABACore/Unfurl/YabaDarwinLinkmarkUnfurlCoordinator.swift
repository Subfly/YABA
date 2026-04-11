//
//  YabaDarwinLinkmarkUnfurlCoordinator.swift
//  YABACore
//
//  Orchestrates HTTP HTML fetch + hidden `converter.html` WebView conversion + asset download.
//

import Foundation

/// Shared pipeline for linkmark creation/detail refresh (Compose parity).
@MainActor
public final class YabaDarwinLinkmarkUnfurlCoordinator {
    public static let shared = YabaDarwinLinkmarkUnfurlCoordinator()

    private var converterRuntime: YabaWKWebViewRuntime?

    private init() {}

    /// Fetches raw HTML, runs the web converter, then downloads reader assets and rewrites JSON.
    public func fetchAndConvert(urlString: String) async throws -> (
        converter: YabaDarwinWebConverterResult,
        readable: YabaDarwinReadableUnfurl
    ) {
        let fetch = try await YabaDarwinUnfurler.fetchRawHtml(urlString)
        let rt = try await ensureConverterRuntime()
        guard await YabaDarwinHtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600) else {
            throw YabaDarwinUnfurlError.converterBridgeNotReady
        }
        let conv = try await YabaDarwinHtmlConversionRunner.run(
            runtime: rt,
            html: fetch.html,
            baseUrl: fetch.normalizedUrl
        )
        let readable = await DarwinConverterResultProcessor.process(
            documentJson: conv.documentJson,
            assets: conv.assets
        )
        return (conv, readable)
    }

    private func ensureConverterRuntime() async throws -> YabaWKWebViewRuntime {
        if let r = converterRuntime {
            return r
        }
        let rt = YabaWKWebViewRuntime(configuration: YabaDarwinWebRuntimeConfiguration())
        rt.loadBundledShell(for: .htmlConverter(inputHtml: nil, baseUrl: nil))
        let ok = await YabaDarwinHtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600)
        guard ok else {
            throw YabaDarwinUnfurlError.converterBridgeNotReady
        }
        converterRuntime = rt
        return rt
    }
}
