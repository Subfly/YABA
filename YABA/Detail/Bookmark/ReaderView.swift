//
//  ReaderView.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//


import SwiftUI
import WebKit

struct ReaderView: View {
    let html: String

    var body: some View {
        WebViewRepresentable(html: html)
            .padding()
    }
}

struct WebViewRepresentable: UIViewRepresentable {
    let html: String

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.loadHTMLString(html, baseURL: nil)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.loadHTMLString(html, baseURL: nil)
    }
}
