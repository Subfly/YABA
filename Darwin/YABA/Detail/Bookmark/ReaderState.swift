//
//  ReaderState.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//

import Foundation
import SwiftUI
import WebKit

internal enum ReaderFontSize: String, CaseIterable {
    case small, medium, large
    
    var css: String {
        switch self {
        case .small: return "font-size: 16px;"
        case .medium: return "font-size: 18px;"
        case .large: return "font-size: 22px;"
        }
    }
}

internal enum ReaderTheme: String, CaseIterable {
    case system, light, dark, sepia
    
    var css: String {
        switch self {
        case .system:
            return ""
        case .light:
            return "--background-color: #ffffff; --text-color: #000000;"
        case .dark:
            return "--background-color: #1e1e1e; --text-color: #e0e0e0;"
        case .sepia:
            return "--background-color: #f4ecd8; --text-color: #5b4636;"
        }
    }
}

internal enum ReaderLineHeight: String, CaseIterable {
    case normal, relaxed
    
    var css: String {
        switch self {
        case .normal: return "line-height: 1.6;"
        case .relaxed: return "line-height: 1.8;"
        }
    }
}

@MainActor
@Observable
internal class ReaderState: NSObject {
    private var webView: WKWebView?
    
    var extractedHTML: String?
    var isLoading: Bool = false
    var readerAvailable: Bool = false
    
    var fontSize: ReaderFontSize = .medium
    var theme: ReaderTheme = .system
    var lineHeight: ReaderLineHeight = .normal
    
    func changeFontSize() {
        withAnimation {
            fontSize = switch fontSize {
            case .small: .medium
            case .medium: .large
            case .large: .small
            }
            
            if let html = extractedHTML {
                extractedHTML = styledHTML(with: html)
            }
        }
    }
    
    func changeTheme() {
        withAnimation {
            theme = switch theme {
            case .system: .dark
            case .dark: .light
            case .light: .sepia
            case .sepia: .system
            }
            
            if let html = extractedHTML {
                extractedHTML = styledHTML(with: html)
            }
        }
    }
    
    func changeLineHeight() {
        withAnimation {
            lineHeight = switch lineHeight {
            case .normal: .relaxed
            case .relaxed: .normal
            }
            
            if let html = extractedHTML {
                extractedHTML = styledHTML(with: html)
            }
        }
    }

    func extractReadableContent(from html: String) {
        isLoading = true
        readerAvailable = false
        extractedHTML = nil

        let config = WKWebViewConfiguration()
        let contentController = WKUserContentController()
        config.userContentController = contentController

        let webView = WKWebView(frame: .zero, configuration: config)
        self.webView = webView
        webView.navigationDelegate = self
        contentController.add(self, name: "readabilityResult")

        webView.loadHTMLString(html, baseURL: nil)
    }
    
    func styledHTML(with content: String) -> String {
        let styleOverrides = """
        body {
            \(fontSize.css)
            \(lineHeight.css)
            \(theme.css)
        }
        """
        return styling
            .replacingOccurrences(of: "%CONTENT%", with: content)
            .replacingOccurrences(of: "</style>", with: "\(styleOverrides)</style>")
    }

    private func injectReadabilityAndExtract() {
        guard let readabilityPath = Bundle.main.path(forResource: "Readability", ofType: "js") else {
            print("❌ Readability.js not found")
            return
        }

        do {
            let script = try String(contentsOfFile: readabilityPath, encoding: .utf8)
            let js = """
                (function() {
                    \(script)
                    const article = new Readability(document.cloneNode(true)).parse();
                    if (article && article.content) {
                        window.webkit.messageHandlers.readabilityResult.postMessage(article.content);
                    } else {
                        window.webkit.messageHandlers.readabilityResult.postMessage("__READABILITY_FAILED__");
                    }
                })();
                """
            webView?.evaluateJavaScript(js)
        } catch {
            print("❌ Failed to load Readability.js: \(error)")
        }
    }
}

extension ReaderState: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        injectReadabilityAndExtract()
    }
}

extension ReaderState: WKScriptMessageHandler {
    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        if message.name == "readabilityResult", let html = message.body as? String {
            if html == "__READABILITY_FAILED__" || html.count < 250 {
                readerAvailable = false
                extractedHTML = nil
            } else {
                readerAvailable = true
                extractedHTML = html
            }

            isLoading = false

            // Cleanup
            webView?.navigationDelegate = nil
            webView = nil
        }
    }
}

private let styling: String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Reader View</title>
  <style>
    :root {
      color-scheme: light dark;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      margin: 0;
      padding: 2rem;
      max-width: 700px;
      margin-left: auto;
      margin-right: auto;
      background: var(--background-color, #fff);
      color: var(--text-color, #000);
      user-select: text;
    }

    @media (prefers-color-scheme: dark) {
      body {
        --background-color: #1e1e1e;
        --text-color: #e0e0e0;
      }

      a {
        color: #81aaff;
      }
    }

    h1, h2, h3 {
      line-height: 1.25;
      margin-top: 2em;
      margin-bottom: 0.5em;
    }

    p {
      margin-bottom: 1.2em;
    }

    img {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
    }

    table {
      border-collapse: collapse;
      width: 100%;
      margin: 1.5em 0;
    }

    th, td {
      border: 1px solid #ccc;
      padding: 0.6em;
      text-align: left;
    }

    ul, ol {
      padding-left: 1.5em;
    }

    a {
      color: inherit;
      text-decoration: none;
      pointer-events: none;
      cursor: default;
    }

    a:hover {
      text-decoration: none;
    }

    code {
      font-family: monospace;
      background-color: rgba(27,31,35,0.05);
      padding: 0.2em 0.4em;
      border-radius: 4px;
    }

    blockquote {
      margin: 1em 0;
      padding-left: 1em;
      border-left: 3px solid #ccc;
      color: #666;
    }
  </style>
</head>
<body>
  <article>
    %CONTENT%
  </article>
</body>
</html>
"""
