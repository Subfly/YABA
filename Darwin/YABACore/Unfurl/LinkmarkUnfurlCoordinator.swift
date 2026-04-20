//
//  LinkmarkUnfurlCoordinator.swift
//  YABACore
//
//  Orchestrates HTTP HTML fetch + hidden `converter.html` WebView conversion + asset download.
//

import Foundation

/// Shared pipeline for linkmark creation/detail refresh (Compose parity).
@MainActor
public final class LinkmarkUnfurlCoordinator {
    public static let shared = LinkmarkUnfurlCoordinator()

    private var converterRuntime: WKWebViewRuntime?

    private init() {}

    /// Fetches raw HTML, runs the web converter, then downloads reader assets and rewrites JSON.
    public func fetchAndConvert(urlString: String) async throws -> (
        converter: WebConverterResult,
        readable: ReadableUnfurl
    ) {
        let fetch = try await Unfurler.fetchRawHtml(urlString)
        let rt = try await ensureConverterRuntime()
        guard await HtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600) else {
            throw UnfurlError.converterBridgeNotReady
        }
        let conv = try await HtmlConversionRunner.run(
            runtime: rt,
            html: fetch.html,
            baseUrl: fetch.normalizedUrl
        )
        let readable = await ConverterResultProcessor.process(
            markdown: conv.markdown,
            assets: conv.assets
        )
        return (conv, readable)
    }

    private func ensureConverterRuntime() async throws -> WKWebViewRuntime {
        if let r = converterRuntime {
            return r
        }
        let rt = WKWebViewRuntime(configuration: WebRuntimeConfiguration())
        rt.loadBundledShell(for: .htmlConverter(inputHtml: nil, baseUrl: nil))
        let ok = await HtmlConversionRunner.waitForConverterBridge(runtime: rt, maxAttempts: 600)
        guard ok else {
            throw UnfurlError.converterBridgeNotReady
        }
        converterRuntime = rt
        return rt
    }
}
