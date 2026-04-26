//
//  Created by Ali Taha on 20.04.2026.
//

import QuartzCore
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
    var onAnnotationTap: (String) -> Void
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
        
        coordinator.runtime.onHostEvent = { [weak coordinator] ev in
            coordinator?.handleHostEventForReadiness(ev)
            coordinator?.parentSnapshot.onHostEvent(ev)
        }
        coordinator.runtime.onAnnotationTap = { [weak coordinator] annotationId in
            coordinator?.parentSnapshot.onAnnotationTap(annotationId)
        }
        coordinator.runtime.onBridgeReady = { [weak coordinator] in
            guard let coordinator else { return }
            coordinator.isBridgeReady = true
            coordinator.attachScrollDirectionPanIfNeeded()
            coordinator.parentSnapshot.onBridgeReady()
            coordinator.queueApply()
        }
        coordinator.runtime.loadBundledViewerShell(
            platform: .darwin,
            appearance: Self.webAppearance(for: colorScheme),
            bundle: .main
        )

        // Full-bleed: avoid UIKit adding safe-area insets; CSS / `setWebChromeInsets` controls padding.
        wv.scrollView.contentInsetAdjustmentBehavior = .never
        wv.scrollView.automaticallyAdjustsScrollIndicatorInsets = false
        // Reader scrolls inside the page (`.yaba-editor-container`), not `WKWebView.scrollView` — use a pan, like Android.
        coordinator.attachScrollDirectionPanIfNeeded()

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
    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var parentSnapshot: LinkmarkReadableWebView
        let runtime: WKWebViewRuntime
        var isBridgeReady = false
        private var linkmarkScrollDirectionPan: UIPanGestureRecognizer?
        private var lastPanTranslationY: CGFloat?
        private var directionAccumulation: CGFloat = 0
        private var ignoreScrollDirectionUntil: CFTimeInterval = 0
        private var lastReloadToken: UUID?
        private var lastDocumentJsonApplied: String?
        private var handledToc: LinkmarkWebTocNavigation?
        private var handledAnnotationScroll: String?

        private static let directionDeltaThreshold: CGFloat = 8

        init(parentSnapshot: LinkmarkReadableWebView, runtime: WKWebViewRuntime) {
            self.parentSnapshot = parentSnapshot
            self.runtime = runtime
        }

        /// Reader content scrolls in an inner `overflow: auto` column; the outer `scrollView` does not `scrollViewDidScroll` — pan on the scroll view (same idea as Android touch on the web view).
        func attachScrollDirectionPanIfNeeded() {
            let wv = runtime.webView
            let sv = wv.scrollView
            if let p = linkmarkScrollDirectionPan, sv.gestureRecognizers?.contains(p) == true { return }
            if let p = linkmarkScrollDirectionPan { p.view?.removeGestureRecognizer(p) }
            let pan = UIPanGestureRecognizer(target: self, action: #selector(handleLinkmarkScrollPan(_:)))
            pan.cancelsTouchesInView = false
            pan.delegate = self
            sv.addGestureRecognizer(pan)
            linkmarkScrollDirectionPan = pan
        }

        private func beginIgnoringScrollDirectionForProgrammaticScroll(duration: CFTimeInterval) {
            ignoreScrollDirectionUntil = CACurrentMediaTime() + duration
            directionAccumulation = 0
            lastPanTranslationY = nil
        }

        private func resetUserScrollDirectionState() {
            directionAccumulation = 0
            lastPanTranslationY = nil
        }

        /// Resets when the load fails or the renderer dies — not on every `.loading` (that can be delivered
        /// out of order or after a successful `bridgeReady`, and would clear `isBridgeReady` incorrectly).
        func handleHostEventForReadiness(_ event: WebHostEvent) {
            if case .loadState(let state) = event {
                switch state {
                case .idle, .rendererCrashed:
                    isBridgeReady = false
                    lastReloadToken = nil
                    lastDocumentJsonApplied = nil
                    resetUserScrollDirectionState()
                case .loading, .pageFinished, .bridgeReady:
                    break
                }
            }
        }

        @objc
        private func handleLinkmarkScrollPan(_ gr: UIPanGestureRecognizer) {
            guard let v = gr.view else { return }
            if CACurrentMediaTime() < ignoreScrollDirectionUntil {
                if gr.state == .ended || gr.state == .cancelled { resetUserScrollDirectionState() }
                return
            }
            let t = gr.translation(in: v).y
            switch gr.state {
            case .began:
                lastPanTranslationY = t
                directionAccumulation = 0
            case .changed:
                if lastPanTranslationY == nil { lastPanTranslationY = t; return }
                let oldT = lastPanTranslationY!
                lastPanTranslationY = t
                let step = t - oldT
                if step == 0 { return }
                // Map pan to the same “content scroll” sense as the old `contentOffset` path: read forward (finger up) → .down.
                let logical = -step
                if directionAccumulation != 0, (logical > 0) != (directionAccumulation > 0) {
                    directionAccumulation = 0
                }
                directionAccumulation += logical
                guard abs(directionAccumulation) >= Self.directionDeltaThreshold else { return }
                if directionAccumulation > 0 {
                    parentSnapshot.onScrollDirection(.down)
                } else {
                    parentSnapshot.onScrollDirection(.up)
                }
                directionAccumulation = 0
            case .ended, .cancelled, .failed:
                lastPanTranslationY = nil
                directionAccumulation = 0
            default:
                break
            }
        }

        // MARK: UIGestureRecognizerDelegate

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            true
        }

        func detach() {
            if let pan = linkmarkScrollDirectionPan {
                pan.view?.removeGestureRecognizer(pan)
                linkmarkScrollDirectionPan = nil
            }
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
                beginIgnoringScrollDirectionForProgrammaticScroll(duration: 0.6)
                Task { @MainActor in
                    let s = WebViewerBridgeScripts.navigateToTocItem(id: toc.id, extrasJson: toc.extrasJson)
                    await self.evalBridgeScript("navigateToTocItem", s)
                    self.parentSnapshot.onTocNavigationConsumed()
                }
            }
            if let aid = parentSnapshot.scrollToAnnotationId, aid != handledAnnotationScroll {
                handledAnnotationScroll = aid
                beginIgnoringScrollDirectionForProgrammaticScroll(duration: 0.6)
                Task { @MainActor in
                    let s = WebViewerBridgeScripts.scrollToAnnotation(annotationId: aid)
                    await self.evalBridgeScript("scrollToAnnotation", s)
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
                let appearance = LinkmarkReadableWebView.webAppearance(for: p.colorScheme)
                await evalBridgeScript(
                    "applyReaderHostPreferences",
                    WebViewerBridgeScripts.applyReaderHostPreferences(
                        platform: .darwin,
                        appearance: appearance,
                        prefs: p.readerPreferences
                    )
                )
                await evalBridgeScript("setWebChromeInsets", WebViewerBridgeScripts.setWebChromeInsets(topPx: p.topChromeInsetPoints))
                await evalBridgeScript("setAnnotations", WebViewerBridgeScripts.setAnnotations(jsonArrayBody: p.annotationsJson))
                await evalBridgeScript("installEditorAnnotationTapHandler", WebViewerBridgeScripts.installEditorAnnotationTapHandler())
                await evalBridgeScript("disableViewportZoom", WebViewerBridgeScripts.disableViewportZoom())
                return
            }
            beginIgnoringScrollDirectionForProgrammaticScroll(duration: 0.45)
            lastReloadToken = p.documentReloadToken
            lastDocumentJsonApplied = p.documentJson
            // Order matches Compose `YabaReadableViewerFeatureHost` (document → read-only → tap wiring → layers → chrome).
            await evalBridgeScript("setDocumentJson", WebViewerBridgeScripts.setDocumentJson(documentJson: p.documentJson, assetsBaseUrl: p.assetsBaseUrl))
            await evalBridgeScript("setEditable(false)", WebViewerBridgeScripts.setEditable(false))
            await evalBridgeScript("installEditorAnnotationTapHandler", WebViewerBridgeScripts.installEditorAnnotationTapHandler())
            await evalBridgeScript("setAnnotations", WebViewerBridgeScripts.setAnnotations(jsonArrayBody: p.annotationsJson))
            let appearance = LinkmarkReadableWebView.webAppearance(for: p.colorScheme)
            await evalBridgeScript(
                "applyReaderHostPreferences",
                WebViewerBridgeScripts.applyReaderHostPreferences(
                    platform: .darwin,
                    appearance: appearance,
                    prefs: p.readerPreferences
                )
            )
            await evalBridgeScript("setWebChromeInsets", WebViewerBridgeScripts.setWebChromeInsets(topPx: p.topChromeInsetPoints))
            await evalBridgeScript("disableViewportZoom", WebViewerBridgeScripts.disableViewportZoom())
        }

        private func evalBridgeScript(_ label: String, _ script: String) async {
            do {
                let r = try await runtime.evaluateJavaScriptStringResult(script)
                #if DEBUG
                if r != "ok", r != "no_bridge" {
                    print("[LinkmarkReadableWebView] \(label): \(r)")
                }
                if r == "no_bridge" {
                    print("[LinkmarkReadableWebView] \(label): YabaEditorBridge not ready (no_bridge)")
                }
                #endif
            } catch {
                #if DEBUG
                print("[LinkmarkReadableWebView] \(label) error: \(error.localizedDescription)")
                #endif
            }
        }
    }
}

struct LinkmarkWebTocNavigation: Equatable {
    var id: String
    var extrasJson: String?
}
