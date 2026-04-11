//
//  YabaWKWebViewRuntime.swift
//  YABACore
//
//  Shared `WKWebView` host: bridge injection, message routing, navigation policy, JS evaluation.
//

import Foundation
import WebKit

/// Owns a configured `WKWebView` for YABA web-component shells. Not tied to SwiftUI.
/// Callbacks are delivered on the main queue.
public final class YabaWKWebViewRuntime: NSObject {
    public let webView: WKWebView
    public private(set) var expectedBridgeFeature: String?

    public var onHostEvent: ((YabaDarwinWebHostEvent) -> Void)?
    public var onBridgeReady: (() -> Void)?
    public var onAnnotationTap: ((String) -> Void)?
    public var onMathTap: ((YabaDarwinMathTapEvent) -> Void)?
    public var onInlineLinkTap: ((YabaDarwinInlineLinkTapEvent) -> Void)?
    public var onInlineMentionTap: ((YabaDarwinInlineMentionTapEvent) -> Void)?

    public var onLoadProgress: ((Double) -> Void)?

    private let configuration: YabaDarwinWebRuntimeConfiguration
    private let scriptBridge = ScriptBridgeProxy()
    private let navProxy = NavigationProxy()
    private let uiProxy = UIDelegateProxy()

    public init(configuration: YabaDarwinWebRuntimeConfiguration = YabaDarwinWebRuntimeConfiguration()) {
        self.configuration = configuration
        let config = WKWebViewConfiguration()
        config.websiteDataStore = configuration.websiteDataStore
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        if let assetHandler = configuration.yabaAssetSchemeHandler {
            config.setURLSchemeHandler(assetHandler, forURLScheme: "yaba-asset")
        }
        config.userContentController.addUserScript(YabaWKBridgeUserScript.nativeHostBridgeScript())
        config.userContentController.add(scriptBridge, name: YabaNativeHostRouterDarwin.nativeHostScriptMessageName)

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

        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
    }

    deinit {
        webView.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
        webView.configuration.userContentController.removeScriptMessageHandler(forName: YabaNativeHostRouterDarwin.nativeHostScriptMessageName)
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
    public func loadBundledShell(for feature: YabaDarwinWebFeature, bundle: Bundle = .main) {
        expectedBridgeFeature = feature.expectedBridgeFeature
        guard let url = Self.resolveShellURL(for: feature, bundle: bundle),
              let readAccess = BundleReader.webComponentsBaseURL(in: bundle) else {
            onHostEvent?(.loadState(.idle))
            return
        }
        onHostEvent?(.loadState(.loading(progressFraction: 0)))
        webView.loadFileURL(url, allowingReadAccessTo: readAccess)
    }

    /// Evaluates JavaScript and returns the JSON-string decoded result (Compose parity).
    public func evaluateJavaScriptStringResult(_ script: String) async throws -> String {
        try await withCheckedThrowingContinuation { cont in
            webView.evaluateJavaScript(script) { result, error in
                if let error = error {
                    cont.resume(throwing: error)
                    return
                }
                if let s = result as? String {
                    cont.resume(returning: YabaWebJsEscaping.decodeJavaScriptStringResult(s))
                } else if result == nil {
                    cont.resume(returning: "")
                } else {
                    cont.resume(returning: String(describing: result!))
                }
            }
        }
    }

    fileprivate func handleScriptMessage(_ message: WKScriptMessage) {
        guard message.name == YabaNativeHostRouterDarwin.nativeHostScriptMessageName else { return }
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
        let handler = YabaNativeHostRouterDarwin.createMessageHandler(
            expectedBridgeFeature: expectedBridgeFeature,
            onBridgeReady: { [weak self] in
                self?.onBridgeReady?()
                self?.onHostEvent?(.loadState(.bridgeReady))
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

    fileprivate func shouldAllow(navigationAction: WKNavigationAction) -> WKNavigationActionPolicy {
        guard let url = navigationAction.request.url else { return .cancel }
        let scheme = url.scheme?.lowercased() ?? ""

        switch scheme {
        case "about", "blob", "data", "javascript":
            return .allow
        case "file":
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

    private static func resolveShellURL(for feature: YabaDarwinWebFeature, bundle: Bundle) -> URL? {
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

// MARK: - Navigation

private final class NavigationProxy: NSObject, WKNavigationDelegate {
    weak var owner: YabaWKWebViewRuntime?

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.onHostEvent?(.loadState(.loading(progressFraction: nil)))
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.onHostEvent?(.loadState(.pageFinished))
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        DispatchQueue.main.async { [weak self] in
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
            self?.owner?.onHostEvent?(.loadState(.rendererCrashed))
        }
    }
}

// MARK: - UI delegate (deny new windows; deny media capture prompts at delegate level)

private final class UIDelegateProxy: NSObject, WKUIDelegate {
    weak var owner: YabaWKWebViewRuntime?

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
    weak var owner: YabaWKWebViewRuntime?

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.handleScriptMessage(message)
        }
    }
}
