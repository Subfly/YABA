//
//  HTMLToMarkdownProcessor.swift
//  YABACore
//
//  Loads the minified unified + rehype + remark bundle and converts HTML to Markdown on JavaScriptCore
//  (no WKWebView). The bundle is produced by `Extensions/yaba-web-components` (see
//  `html-to-markdown.bundle.min.js`).
//

import Foundation
import JavaScriptCore

public enum HTMLToMarkdownError: Error, Sendable {
    case bundleNotFound
    case bundleReadFailed(String)
    case javaScriptContextInitFailed
    case functionMissing
    case javaScriptException(String)
    case conversionProducedNonString
}

public enum HTMLToMarkdownProcessor: Sendable {
    private static let lock: NSLock = {
        let l = NSLock()
        l.name = "YABA.HTMLToMarkdown"
        return l
    }()

    /// Serializes access with `lock`; do not use off-main without keeping the same isolation as `convert`.
    private static var cachedContext: JSContext?

    /// Converts `html` to CommonMark + GFM markdown using `globalThis.HTMLToMarkdown` from the bundled script (rehype → remark).
    /// - Parameter html: Full HTML (or a fragment) as fetched from the page.
    public static func convert(html: String) throws -> String {
        lock.lock()
        defer { lock.unlock() }

        let context: JSContext
        if let cached = Self.cachedContext {
            context = cached
        } else {
            let newContext = try loadAndEvaluateBundle()
            Self.cachedContext = newContext
            context = newContext
        }
        return try callHTMLToMarkdown(context: context, html: html)
    }

    private static func loadAndEvaluateBundle() throws -> JSContext {
        let ctx = JSContext()!
        // Do not `assertionFailure` here: it crashes the app in DEBUG on normal JS throws (e.g. bad HTML).
        // Errors are returned via `context.exception` after `callHTMLToMarkdown`.
        guard let url = BundleReader.htmlToMarkdownBundleURL() else {
            throw HTMLToMarkdownError.bundleNotFound
        }
        let source: String
        do {
            source = try String(contentsOf: url, encoding: .utf8)
        } catch {
            throw HTMLToMarkdownError.bundleReadFailed(String(describing: error))
        }
        if source.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            throw HTMLToMarkdownError.bundleReadFailed("empty bundle")
        }
        _ = ctx.evaluateScript(source, withSourceURL: url)
        if let exc = ctx.exception, !exc.isUndefined {
            throw HTMLToMarkdownError.javaScriptException(exc.toString() ?? "unknown")
        }
        guard
            let fn = ctx.globalObject?.objectForKeyedSubscript("HTMLToMarkdown"), !fn.isUndefined
        else {
            throw HTMLToMarkdownError.functionMissing
        }
        return ctx
    }

    private static func callHTMLToMarkdown(context: JSContext, html: String) throws -> String {
        guard
            let fn = context.globalObject?.objectForKeyedSubscript("HTMLToMarkdown"), !fn.isUndefined
        else {
            throw HTMLToMarkdownError.functionMissing
        }
        let htmlValue = JSValue(object: html, in: context) ?? JSValue(nullIn: context)
        context.exception = nil
        let result = fn.call(withArguments: [htmlValue])
        if let exc = context.exception, !exc.isUndefined {
            let text = exc.isString ? (exc.toString() ?? "unknown") : (exc.toString() ?? "unknown")
            throw HTMLToMarkdownError.javaScriptException(text)
        }
        guard let result, !result.isUndefined, !result.isNull else {
            throw HTMLToMarkdownError.conversionProducedNonString
        }
        if result.isString, let s = result.toString() { return s }
        if let s = result.toString() { return s }
        throw HTMLToMarkdownError.conversionProducedNonString
    }
}
