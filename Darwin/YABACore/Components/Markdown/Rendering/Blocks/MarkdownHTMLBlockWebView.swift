//
//  MarkdownHTMLBlockWebView.swift
//  YABACore
//
//  Raw HTML block rendering via `WKWebView`.
//

import SwiftUI
import WebKit
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif

// MARK: - HTML WebView

#if canImport(UIKit)
struct MarkdownHTMLBlockWebView: UIViewRepresentable {
    let html: String

    func makeUIView(context: Context) -> WKWebView {
        let c = WKWebViewConfiguration()
        c.defaultWebpagePreferences.allowsContentJavaScript = true
        let w = WKWebView(frame: .zero, configuration: c)
        w.isOpaque = false
        w.scrollView.isScrollEnabled = true
        return w
    }

    func updateUIView(_ w: WKWebView, context: Context) {
        w.loadHTMLString(MarkdownHTMLBlockTemplate.htmlDocument(html: html), baseURL: nil)
    }
}
#elseif canImport(AppKit)
struct MarkdownHTMLBlockWebView: NSViewRepresentable {
    let html: String

    func makeNSView(context: Context) -> WKWebView {
        let c = WKWebViewConfiguration()
        c.defaultWebpagePreferences.allowsContentJavaScript = true
        let w = WKWebView(frame: .zero, configuration: c)
        return w
    }

    func updateNSView(_ w: WKWebView, context: Context) {
        w.loadHTMLString(MarkdownHTMLBlockTemplate.htmlDocument(html: html), baseURL: nil)
    }
}
#endif

private enum MarkdownHTMLBlockTemplate {
    static func htmlDocument(html: String) -> String {
        """
        <!doctype html><html><head><meta name=viewport content="width=device-width,initial-scale=1">
        <style>body { font: -apple-system-body; }
        pre { white-space: pre-wrap; } code { font-family: ui-monospace; }</style>
        </head><body>
        """ + html + "</body></html>"
    }
}
