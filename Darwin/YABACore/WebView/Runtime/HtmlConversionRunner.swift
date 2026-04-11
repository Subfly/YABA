//
//  HtmlConversionRunner.swift
//  YABACore
//
//  Parity with Compose `YabaWebAndroidConverters.runHtmlConversion`.
//

import Foundation

public enum HtmlConversionRunner {
    private static let jobTimeoutNs: UInt64 = 120_000_000_000

    /// Runs HTML → reader `documentJson` conversion after the converter shell and `YabaConverterBridge` are ready.
    @MainActor
    public static func run(
        runtime: WKWebViewRuntime,
        html: String,
        baseUrl: String?
    ) async throws -> WebConverterResult {
        try await withThrowingTaskGroup(of: WebConverterResult.self) { group in
            group.addTask { @MainActor in
                try await runConversion(runtime: runtime, html: html, baseUrl: baseUrl)
            }
            group.addTask {
                try await Task.sleep(nanoseconds: jobTimeoutNs)
                throw UnfurlError.htmlConversionTimedOut
            }
            let first = try await group.next()!
            group.cancelAll()
            return first
        }
    }

    @MainActor
    private static func runConversion(
        runtime: WKWebViewRuntime,
        html: String,
        baseUrl: String?
    ) async throws -> WebConverterResult {
        let jobId = UUID().uuidString
        let result = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<WebConverterResult, Error>) in
            var completed = false
            let lock = NSLock()
            func finish(_ r: Result<WebConverterResult, Error>) {
                lock.lock()
                defer { lock.unlock() }
                guard !completed else { return }
                completed = true
                switch r {
                case let .success(v): cont.resume(returning: v)
                case let .failure(e): cont.resume(throwing: e)
                }
            }
            ConverterJobRegistry.shared.registerHtmlJob(jobId: jobId) { res in
                switch res {
                case let .success(h):
                    finish(.success(h.asWebConverterResult()))
                case let .failure(e):
                    finish(.failure(e))
                }
            }
            Task { @MainActor in
                do {
                    let script = WebBridgeScripts.sanitizeAndConvertHtmlToReaderHtmlScript(
                        html: html,
                        baseUrl: baseUrl,
                        jobId: jobId
                    )
                    let raw = try await runtime.evaluateJavaScriptStringResult(script)
                    let rid = WebJsEscaping.decodeJavaScriptStringResult(raw).trimmingCharacters(in: .whitespacesAndNewlines)
                    if rid.isEmpty || rid != jobId {
                        ConverterJobRegistry.shared.removeHtmlJob(jobId: jobId)
                        finish(.failure(UnfurlError.htmlConversionStartFailed))
                    }
                } catch {
                    ConverterJobRegistry.shared.removeHtmlJob(jobId: jobId)
                    finish(.failure(error))
                }
            }
        }
        defer {
            Task { @MainActor in
                _ = try? await runtime.evaluateJavaScriptStringResult(
                    WebBridgeScripts.deleteHtmlConversionJobScript(jobId: jobId)
                )
            }
        }
        return result
    }

    /// Polls until `window.YabaConverterBridge` exists (converter shell loaded).
    @MainActor
    public static func waitForConverterBridge(runtime: WKWebViewRuntime, maxAttempts: Int = 600) async -> Bool {
        for _ in 0 ..< maxAttempts {
            do {
                let raw = try await runtime.evaluateJavaScriptStringResult(WebBridgeScripts.converterBridgeDefined)
                let s = raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if s == "true" || s == "1" { return true }
            } catch {
                // keep polling
            }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        return false
    }
}
