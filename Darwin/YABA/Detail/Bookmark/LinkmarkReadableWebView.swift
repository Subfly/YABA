//
//  Created by Ali Taha on 20.04.2026.
//

import SwiftUI
import UIKit
import WebKit

/// Hosts `viewer.html` via `WKWebViewRuntime` and applies `YabaEditorBridge` commands.
struct LinkmarkReadableWebView: UIViewRepresentable {
    var webDriver: LinkmarkWebDriver

    var documentJson: String
    var assetsBaseUrl: String
    var annotationsJson: String
    var readerPreferences: ReaderPreferences
    var topChromeInsetPoints: CGFloat
    var colorScheme: ColorScheme
    var documentReloadToken: UUID
    var inlineAssets: [(assetId: String, bytes: Data)]
    var tocNavigate: LinkmarkWebTocNavigation?
    var scrollToAnnotationId: String?
    var onHostEvent: (WebHostEvent) -> Void
    var onScrollDirection: (ScrollAxisDirection) -> Void
    var onBridgeReady: () -> Void
    var onTocNavigationConsumed: () -> Void
    var onScrollToAnnotationConsumed: () -> Void

    enum ScrollAxisDirection {
        case up
        case down
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(
            parentSnapshot: self,
            runtime: WKWebViewRuntime(
                configuration: WebRuntimeConfiguration(
                    yabaAssetSchemeHandler: ReadableAssetSchemeHandler()
                )
            )
        )
    }

    func makeUIView(context: Context) -> WKWebView {
        let coordinator = context.coordinator
        let wv = coordinator.runtime.webView
        webDriver.runtime = coordinator.runtime
        coordinator.attachScrollLogging()
        coordinator.runtime.onHostEvent = { [weak coordinator] ev in
            coordinator?.parentSnapshot.onHostEvent(ev)
        }
        coordinator.runtime.onBridgeReady = { [weak coordinator] in
            guard let coordinator else { return }
            coordinator.isBridgeReady = true
            coordinator.parentSnapshot.onBridgeReady()
            coordinator.queueApply()
        }
        coordinator.runtime.loadBundledViewerShell(
            platform: .darwin,
            appearance: Self.webAppearance(for: colorScheme),
            bundle: .main
        )
        return wv
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.parentSnapshot = self
        webDriver.runtime = context.coordinator.runtime
        context.coordinator.queueApply()
        context.coordinator.handleSideEffects()
    }

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        coordinator.detach()
    }

    private static func webAppearance(for scheme: ColorScheme) -> WebAppearance {
        switch scheme {
        case .dark: return .dark
        case .light: return .light
        @unknown default: return .auto
        }
    }

    @MainActor
    final class Coordinator: NSObject {
        var parentSnapshot: LinkmarkReadableWebView
        let runtime: WKWebViewRuntime
        var isBridgeReady = false
        private var lastContentOffsetY: CGFloat = 0
        private var kvoToken: NSKeyValueObservation?
        private var lastReloadToken: UUID?
        private var lastDocumentJsonApplied: String?
        private var handledToc: LinkmarkWebTocNavigation?
        private var handledAnnotationScroll: String?

        init(parentSnapshot: LinkmarkReadableWebView, runtime: WKWebViewRuntime) {
            self.parentSnapshot = parentSnapshot
            self.runtime = runtime
        }

        func attachScrollLogging() {
            let sv = runtime.webView.scrollView
            kvoToken = sv.observe(\.contentOffset, options: [.new]) { [weak self] scrollView, _ in
                guard let self else { return }
                let y = scrollView.contentOffset.y
                let delta = y - self.lastContentOffsetY
                if abs(delta) < 6 {
                    return
                }
                if delta > 0 {
                    self.parentSnapshot.onScrollDirection(.down)
                } else {
                    self.parentSnapshot.onScrollDirection(.up)
                }
                self.lastContentOffsetY = y
            }
        }

        func detach() {
            kvoToken?.invalidate()
            kvoToken = nil
        }

        func queueApply() {
            guard isBridgeReady else { return }
            Task { @MainActor in
                await self.applyMainDocumentIfNeeded()
            }
        }

        func handleSideEffects() {
            guard isBridgeReady else { return }
            if parentSnapshot.tocNavigate == nil {
                handledToc = nil
            }
            if parentSnapshot.scrollToAnnotationId == nil {
                handledAnnotationScroll = nil
            }
            let toc = parentSnapshot.tocNavigate
            if let toc, toc != handledToc {
                handledToc = toc
                Task { @MainActor in
                    let s = WebViewerBridgeScripts.navigateToTocItem(id: toc.id, extrasJson: toc.extrasJson)
                    _ = try? await self.runtime.evaluateJavaScriptStringResult(s)
                    self.parentSnapshot.onTocNavigationConsumed()
                }
            }
            if let aid = parentSnapshot.scrollToAnnotationId, aid != handledAnnotationScroll {
                handledAnnotationScroll = aid
                Task { @MainActor in
                    let s = WebViewerBridgeScripts.scrollToAnnotation(annotationId: aid)
                    _ = try? await self.runtime.evaluateJavaScriptStringResult(s)
                    self.parentSnapshot.onScrollToAnnotationConsumed()
                }
            }
        }

        private func applyMainDocumentIfNeeded() async {
            let p = parentSnapshot
            for a in p.inlineAssets {
                ReadableAssetResolver.shared.register(assetId: a.assetId, bytes: a.bytes)
            }
            let tokenChanged = lastReloadToken != p.documentReloadToken
            let documentChanged = lastDocumentJsonApplied != p.documentJson
            guard tokenChanged || documentChanged else {
                let prefs = WebViewerBridgeScripts.setReaderPreferences(p.readerPreferences)
                _ = try? await runtime.evaluateJavaScriptStringResult(prefs)
                let chrome = WebViewerBridgeScripts.setWebChromeInsets(topPx: p.topChromeInsetPoints)
                _ = try? await runtime.evaluateJavaScriptStringResult(chrome)
                let ann = WebViewerBridgeScripts.setAnnotations(jsonArrayBody: p.annotationsJson)
                _ = try? await runtime.evaluateJavaScriptStringResult(ann)
                return
            }
            lastReloadToken = p.documentReloadToken
            lastDocumentJsonApplied = p.documentJson
            let script = WebViewerBridgeScripts.setDocumentJson(documentJson: p.documentJson, assetsBaseUrl: p.assetsBaseUrl)
            _ = try? await runtime.evaluateJavaScriptStringResult(script)
            let ann = WebViewerBridgeScripts.setAnnotations(jsonArrayBody: p.annotationsJson)
            _ = try? await runtime.evaluateJavaScriptStringResult(ann)
            let prefs = WebViewerBridgeScripts.setReaderPreferences(p.readerPreferences)
            _ = try? await runtime.evaluateJavaScriptStringResult(prefs)
            let chrome = WebViewerBridgeScripts.setWebChromeInsets(topPx: p.topChromeInsetPoints)
            _ = try? await runtime.evaluateJavaScriptStringResult(chrome)
        }
    }
}

struct LinkmarkWebTocNavigation: Equatable {
    var id: String
    var extrasJson: String?
}
