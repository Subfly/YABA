//
//  WKWebViewRuntime.swift
//  YABACore
//
//  Shared `WKWebView` host: bridge injection, message routing, navigation policy, JS evaluation.
//

import Foundation
import WebKit

/// Owns a configured `WKWebView` for YABA web-component shells. Not tied to SwiftUI.
/// Callbacks are delivered on the main queue.
public final class WKWebViewRuntime: NSObject {
    public let webView: WKWebView
    public private(set) var expectedBridgeFeature: String?

    public var onHostEvent: ((WebHostEvent) -> Void)?
    public var onBridgeReady: (() -> Void)?
    public var onAnnotationTap: ((String) -> Void)?
    public var onMathTap: ((MathTapEvent) -> Void)?
    public var onInlineLinkTap: ((InlineLinkTapEvent) -> Void)?
    public var onInlineMentionTap: ((InlineMentionTapEvent) -> Void)?

    public var onLoadProgress: ((Double) -> Void)?

    private let configuration: WebRuntimeConfiguration
    private let scriptBridge = ScriptBridgeProxy()
    private let navProxy = NavigationProxy()
    private let uiProxy = UIDelegateProxy()

    /// Compose parity: `WebViewClient.onPageFinished` and web `bridgeReady` must both be true before treating the shell as ready.
    private var pageLoadFinishedForCycle = false
    private var webPostedBridgeReadyForCycle = false
    private var emittedCombinedBridgeReadyForCycle = false

    /// Parity with Android `YabaWebBridgeScripts.EDITOR_BRIDGE_READY` / `waitForBridgeReady` when `postMessage` is delayed or not delivered to `WKUserContentController`.
    private static let editorBridgeIsReadyJavaScript =
        """
        (function() {
            try {
                return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady());
            } catch (e) {
                return false;
            }
        })();
        """

    public init(configuration: WebRuntimeConfiguration = WebRuntimeConfiguration()) {
        self.configuration = configuration
        let config = WKWebViewConfiguration()
        config.websiteDataStore = configuration.websiteDataStore
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        if let assetHandler = configuration.yabaAssetSchemeHandler {
            config.setURLSchemeHandler(assetHandler, forURLScheme: "yaba-asset")
        }
        config.userContentController.addUserScript(WKBridgeUserScript.nativeHostBridgeScript())
        config.userContentController.add(scriptBridge, name: NativeHostRouterDarwin.nativeHostScriptMessageName)
        config.preferences.setValue(true, forKey: "allowFileAccessFromFileURLs")

        let wv = WKWebView(frame: .zero, configuration: config)
        self.webView = wv

        super.init()

        scriptBridge.owner = self
        navProxy.owner = self
        uiProxy.owner = self

        webView.navigationDelegate = navProxy
        webView.uiDelegate = uiProxy
        webView.isOpaque = false
        webView.backgroundColor = .clear
        
        #if DEBUG
        if #available(iOS 16.4, macOS 13.3, macCatalyst 16.4, *) {
            webView.isInspectable = true
        }
        #endif

        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
    }

    deinit {
        webView.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
        webView.configuration.userContentController.removeScriptMessageHandler(forName: NativeHostRouterDarwin.nativeHostScriptMessageName)
    }

    public override func observeValue(
        forKeyPath keyPath: String?,
        of object: Any?,
        change: [NSKeyValueChangeKey: Any]?,
        context: UnsafeMutableRawPointer?
    ) {
        if keyPath == #keyPath(WKWebView.estimatedProgress), let w = object as? WKWebView {
            DispatchQueue.main.async { [weak self] in
                self?.onLoadProgress?(w.estimatedProgress)
            }
        }
    }

    /// Resolves the bundled shell URL and loads it with read access to the web-components directory.
    @MainActor
    public func loadBundledShell(for feature: WebFeature, bundle: Bundle = .main) {
        expectedBridgeFeature = feature.expectedBridgeFeature
        guard let url = Self.resolveShellURL(for: feature, bundle: bundle),
              let readAccess = BundleReader.webComponentsBaseURL(in: bundle) else {
            onHostEvent?(.loadState(.idle))
            return
        }
        resetBridgeReadinessCycle()
        onHostEvent?(.loadState(.loading(progressFraction: 0)))
        webView.loadFileURL(url, allowingReadAccessTo: readAccess)
    }

    /// Evaluates JavaScript and returns the JSON-string decoded result (Compose parity).
    @MainActor
    public func evaluateJavaScriptStringResult(_ script: String) async throws -> String {
        try await withCheckedThrowingContinuation { cont in
            webView.evaluateJavaScript(script) { result, error in
                if let error = error {
                    cont.resume(throwing: error)
                    return
                }
                if let s = result as? String {
                    cont.resume(returning: WebJsEscaping.decodeJavaScriptStringResult(s))
                } else if result == nil {
                    cont.resume(returning: "")
                } else {
                    cont.resume(returning: String(describing: result!))
                }
            }
        }
    }

    fileprivate func handleScriptMessage(_ message: WKScriptMessage) {
        guard message.name == NativeHostRouterDarwin.nativeHostScriptMessageName else { return }
        let body: String
        if let s = message.body as? String {
            body = s
        } else if let dict = message.body as? [String: Any],
                  let data = try? JSONSerialization.data(withJSONObject: dict),
                  let str = String(data: data, encoding: .utf8) {
            body = str
        } else {
            return
        }
        dispatchNativeHostJSON(body)
    }

    private func dispatchNativeHostJSON(_ json: String) {
        let handler = NativeHostRouterDarwin.createMessageHandler(
            expectedBridgeFeature: expectedBridgeFeature,
            onBridgeReady: { [weak self] in
                self?.markWebPostedBridgeReadyFromWeb()
            },
            onHostEvent: { [weak self] event in
                self?.onHostEvent?(event)
            },
            onAnnotationTap: { [weak self] id in
                self?.onAnnotationTap?(id)
            },
            onMathTap: { [weak self] ev in
                self?.onMathTap?(ev)
            },
            onInlineLinkTap: { [weak self] ev in
                self?.onInlineLinkTap?(ev)
            },
            onInlineMentionTap: { [weak self] ev in
                self?.onInlineMentionTap?(ev)
            }
        )
        handler(json)
    }

    @MainActor
    fileprivate func resetBridgeReadinessCycle() {
        pageLoadFinishedForCycle = false
        webPostedBridgeReadyForCycle = false
        emittedCombinedBridgeReadyForCycle = false
    }

    /// Web posted `bridgeReady` for `expectedBridgeFeature`; emit app-level ready only after `WKNavigationDelegate` page load finished.
    fileprivate func markWebPostedBridgeReadyFromWeb() {
        webPostedBridgeReadyForCycle = true
        emitCombinedBridgeReadyIfNeeded()
    }

    fileprivate func markPageLoadFinishedForBridgeReadiness() {
        pageLoadFinishedForCycle = true
        emitCombinedBridgeReadyIfNeeded()
        startEditorBridgeReadinessProbingIfNeeded()
    }

    /// If the web layer never posts `bridgeReady` to `window.webkit.messageHandlers` (but TipTap is up), mark ready from JS, same as Android polling.
    fileprivate func startEditorBridgeReadinessProbingIfNeeded() {
        guard !emittedCombinedBridgeReadyForCycle else { return }
        Task { @MainActor [weak self] in
            guard let self else { return }
            for _ in 0 ..< 50 {
                if self.emittedCombinedBridgeReadyForCycle { return }
                if await self.checkEditorBridgeReadyViaJavaScript() {
                    self.markWebPostedBridgeReadyFromWeb()
                    return
                }
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    @MainActor
    fileprivate func checkEditorBridgeReadyViaJavaScript() async -> Bool {
        await withCheckedContinuation { (cont: CheckedContinuation<Bool, Never>) in
            self.webView.evaluateJavaScript(Self.editorBridgeIsReadyJavaScript) { result, _ in
                if let b = result as? Bool {
                    cont.resume(returning: b)
                } else if let n = result as? NSNumber {
                    cont.resume(returning: n.boolValue)
                } else {
                    cont.resume(returning: false)
                }
            }
        }
    }

    fileprivate func emitCombinedBridgeReadyIfNeeded() {
        guard pageLoadFinishedForCycle, webPostedBridgeReadyForCycle, !emittedCombinedBridgeReadyForCycle else { return }
        emittedCombinedBridgeReadyForCycle = true
        onBridgeReady?()
        onHostEvent?(.loadState(.bridgeReady))
    }

    fileprivate func shouldAllow(navigationAction: WKNavigationAction) -> WKNavigationActionPolicy {
        guard let url = navigationAction.request.url else { return .cancel }
        let scheme = url.scheme?.lowercased() ?? ""

        switch scheme {
        case "about", "blob", "data", "javascript":
            return .allow
        case "file":
            return .allow
        case "yaba-asset":
            // Readable inline assets (`ReadableViewerAssets`) are served by `WKURLSchemeHandler`.
            return .allow
        case "http", "https":
            if configuration.allowsRemoteNavigation || configuration.allowsRemoteHTTP {
                return .allow
            }
            return .cancel
        default:
            return .cancel
        }
    }

    private static func resolveShellURL(for feature: WebFeature, bundle: Bundle) -> URL? {
        switch feature {
        case .readableViewer:
            return BundleReader.getViewerURL(in: bundle)
        case .editor:
            return BundleReader.getEditorURL(in: bundle)
        case .canvas:
            return BundleReader.getCanvasURL(in: bundle)
        case .htmlConverter, .pdfExtractor, .epubExtractor:
            return BundleReader.getConverterURL(in: bundle)
        case .pdfViewer:
            return BundleReader.getPdfViewerURL(in: bundle)
        case .epubViewer:
            return BundleReader.getEpubViewerURL(in: bundle)
        }
    }

}

// MARK: - Readable viewer shell

public extension WKWebViewRuntime {
    /// Loads `viewer.html` with platform/appearance query params (`yaba-web-components` / Milkdown Crepe).
    @MainActor
    func loadBundledViewerShell(
        platform: WebPlatform = .darwin,
        appearance: WebAppearance = .auto,
        cursor: String? = nil,
        bundle: Bundle = .main
    ) {
        expectedBridgeFeature = "viewer"
        guard let fileURL = BundleReader.viewerURLWithQuery(
            platform: platform,
            appearance: appearance,
            cursor: cursor,
            bundle: bundle
        ),
            let readAccess = BundleReader.webComponentsBaseURL(in: bundle)
        else {
            onHostEvent?(.loadState(.idle))
            return
        }
        resetBridgeReadinessCycle()
        onHostEvent?(.loadState(.loading(progressFraction: 0)))
        webView.loadFileURL(fileURL, allowingReadAccessTo: readAccess)
    }
}

// MARK: - Navigation

private final class NavigationProxy: NSObject, WKNavigationDelegate {
    weak var owner: WKWebViewRuntime?

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        // Do NOT call `resetBridgeReadinessCycle` here. It is already invoked in `loadBundledShell` /
        // `loadBundledViewerShell` before `loadFileURL`, and in `didFail` / `webViewWebContentProcessDidTerminate`.
        //
        // `didStartProvisionalNavigation` is delivered asynchronously. If it runs *after* the page has
        // already posted `bridgeReady` but *before* `didFinish`, resetting would clear
        // `webPostedBridgeReadyForCycle` and the combined `onBridgeReady` would never fire.
        DispatchQueue.main.async { [weak self] in
            self?.owner?.onHostEvent?(.loadState(.loading(progressFraction: nil)))
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.async { [weak self] in
            guard let owner = self?.owner else { return }
            owner.onHostEvent?(.loadState(.pageFinished))
            owner.markPageLoadFinishedForBridgeReadiness()
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.resetBridgeReadinessCycle()
            self?.owner?.onHostEvent?(.loadState(.idle))
        }
    }

    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation navigation: WKNavigation!,
        withError error: Error
    ) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.resetBridgeReadinessCycle()
            self?.owner?.onHostEvent?(.loadState(.idle))
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let owner else {
            decisionHandler(.cancel)
            return
        }
        DispatchQueue.main.async {
            decisionHandler(owner.shouldAllow(navigationAction: navigationAction))
        }
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.resetBridgeReadinessCycle()
            self?.owner?.onHostEvent?(.loadState(.rendererCrashed))
        }
    }
}

// MARK: - UI delegate (deny new windows; deny media capture prompts at delegate level)

private final class UIDelegateProxy: NSObject, WKUIDelegate {
    weak var owner: WKWebViewRuntime?

    func webView(
        _ webView: WKWebView,
        createWebViewWith configuration: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
        nil
    }

    @available(iOS 15.0, macCatalyst 15.0, *)
    func webView(
        _ webView: WKWebView,
        requestMediaCapturePermissionFor origin: WKSecurityOrigin,
        initiatedByFrame frame: WKFrameInfo,
        type: WKMediaCaptureType,
        decisionHandler: @escaping (WKPermissionDecision) -> Void
    ) {
        decisionHandler(.deny)
    }
}

// MARK: - Script message

private final class ScriptBridgeProxy: NSObject, WKScriptMessageHandler {
    weak var owner: WKWebViewRuntime?

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.handleScriptMessage(message)
        }
    }
}
