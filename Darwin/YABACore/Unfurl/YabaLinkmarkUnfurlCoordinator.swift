//
//  YabaLinkmarkUnfurlCoordinator.swift
//  YABACore
//
//  Orchestrates HTTP HTML fetch + hidden `converter.html` WebView conversion + asset download.
//

import Foundation

/// Shared pipeline for linkmark creation/detail refresh (Compose parity).
@MainActor
public final class YabaLinkmarkUnfurlCoordinator {
    public static let shared = YabaLinkmarkUnfurlCoordinator()

    private var converterRuntime: YabaWKWebViewRuntime?

    private init() {}

    /// Fetches raw HTML, runs the web converter, then downloads reader assets and rewrites JSON.
    public func fetchAndConvert(urlString: String) async throws -> (
        converter: YabaWebConverterResult,
        readable: YabaReadableUnfurl
    ) {
        let fetch = try await YabaUnfurler.fetchRawHtml(urlString)
        let rt = try await ensureConverterRuntime()
        guard await YabaHtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600) else {
            throw YabaUnfurlError.converterBridgeNotReady
        }
        let conv = try await YabaHtmlConversionRunner.run(
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
        let rt = YabaWKWebViewRuntime(configuration: YabaWebRuntimeConfiguration())
        rt.loadBundledShell(for: .htmlConverter(inputHtml: nil, baseUrl: nil))
        let ok = await YabaHtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600)
        guard ok else {
            throw YabaUnfurlError.converterBridgeNotReady
        }
        converterRuntime = rt
        return rt
    }
}
