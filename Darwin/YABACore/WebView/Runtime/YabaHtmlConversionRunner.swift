//
//  YabaHtmlConversionRunner.swift
//  YABACore
//
//  Parity with Compose `YabaWebAndroidConverters.runHtmlConversion`.
//

import Foundation

public enum YabaHtmlConversionRunner {
    private static let jobTimeoutNs: UInt64 = 120_000_000_000

    /// Runs HTML → reader `documentJson` conversion after the converter shell and `YabaConverterBridge` are ready.
    @MainActor
    public static func run(
        runtime: YabaWKWebViewRuntime,
        html: String,
        baseUrl: String?
    ) async throws -> YabaWebConverterResult {
        try await withThrowingTaskGroup(of: YabaWebConverterResult.self) { group in
            group.addTask { @MainActor in
                try await runConversion(runtime: runtime, html: html, baseUrl: baseUrl)
            }
            group.addTask {
                try await Task.sleep(nanoseconds: jobTimeoutNs)
                throw YabaUnfurlError.htmlConversionTimedOut
            }
            let first = try await group.next()!
            group.cancelAll()
            return first
        }
    }

    @MainActor
    private static func runConversion(
        runtime: YabaWKWebViewRuntime,
        html: String,
        baseUrl: String?
    ) async throws -> YabaWebConverterResult {
        let jobId = UUID().uuidString
        let result = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<YabaWebConverterResult, Error>) in
            var completed = false
            let lock = NSLock()
            func finish(_ r: Result<YabaWebConverterResult, Error>) {
                lock.lock()
                defer { lock.unlock() }
                guard !completed else { return }
                completed = true
                switch r {
                case let .success(v): cont.resume(returning: v)
                case let .failure(e): cont.resume(throwing: e)
                }
            }
            YabaConverterJobRegistry.shared.registerHtmlJob(jobId: jobId) { res in
                switch res {
                case let .success(h):
                    finish(.success(h.asWebConverterResult()))
                case let .failure(e):
                    finish(.failure(e))
                }
            }
            Task { @MainActor in
                do {
                    let script = YabaWebBridgeScripts.sanitizeAndConvertHtmlToReaderHtmlScript(
                        html: html,
                        baseUrl: baseUrl,
                        jobId: jobId
                    )
                    let raw = try await runtime.evaluateJavaScriptStringResult(script)
                    let rid = YabaWebJsEscaping.decodeJavaScriptStringResult(raw).trimmingCharacters(in: .whitespacesAndNewlines)
                    if rid.isEmpty || rid != jobId {
                        YabaConverterJobRegistry.shared.removeHtmlJob(jobId: jobId)
                        finish(.failure(YabaUnfurlError.htmlConversionStartFailed))
                    }
                } catch {
                    YabaConverterJobRegistry.shared.removeHtmlJob(jobId: jobId)
                    finish(.failure(error))
                }
            }
        }
        defer {
            Task { @MainActor in
                _ = try? await runtime.evaluateJavaScriptStringResult(
                    YabaWebBridgeScripts.deleteHtmlConversionJobScript(jobId: jobId)
                )
            }
        }
        return result
    }

    /// Polls until `window.YabaConverterBridge` exists (converter shell loaded).
    @MainActor
    public static func waitForConverterBridge(runtime: YabaWKWebViewRuntime, maxAttempts: Int = 600) async -> Bool {
        for _ in 0 ..< maxAttempts {
            do {
                let raw = try await runtime.evaluateJavaScriptStringResult(YabaWebBridgeScripts.converterBridgeDefined)
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
