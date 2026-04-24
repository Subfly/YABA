//
//  DocumentExtractionRunner.swift
//  YABACore
//
//  Parity with Compose `YabaWebAndroidConverters.runPdfExtraction` / `runEpubExtraction`.
//

import Foundation

public enum DocumentExtractionRunner {
    private static let jobTimeoutNs: UInt64 = 120_000_000_000

    @MainActor
    public static func runPdfExtraction(
        runtime: WKWebViewRuntime,
        pdfDataUrl: String,
        renderScale: Float = 1.2
    ) async throws -> WebPdfConverterResult {
        guard await HtmlConversionRunner.waitForConverterBridge(runtime: runtime, maxAttempts: 600) else {
            throw UnfurlError.converterBridgeNotReady
        }
        return try await withThrowingTaskGroup(of: WebPdfConverterResult.self) { group in
            group.addTask { @MainActor in
                try await runPdfExtractionInner(runtime: runtime, pdfDataUrl: pdfDataUrl, renderScale: renderScale)
            }
            group.addTask {
                try await Task.sleep(nanoseconds: jobTimeoutNs)
                throw UnfurlError.pdfExtractionTimedOut
            }
            let first = try await group.next()!
            group.cancelAll()
            return first
        }
    }

    @MainActor
    private static func runPdfExtractionInner(
        runtime: WKWebViewRuntime,
        pdfDataUrl: String,
        renderScale: Float
    ) async throws -> WebPdfConverterResult {
        let cleanup = ExtractionJobIdBox()
        let result = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<WebPdfConverterResult, Error>) in
            Task { @MainActor in
                do {
                    let script = WebBridgeScripts.startPdfExtractionScript(
                        resolvedPdfUrl: pdfDataUrl,
                        renderScale: renderScale
                    )
                    let raw = try await runtime.evaluateJavaScriptStringResult(script)
                    let jobId = WebJsEscaping.decodeJavaScriptStringResult(raw)
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    if jobId.isEmpty {
                        cont.resume(throwing: UnfurlError.pdfExtractionStartFailed)
                        return
                    }
                    cleanup.jobId = jobId
                    ConverterJobRegistry.shared.registerPdfJob(jobId: jobId) { res in
                        switch res {
                        case let .success(outputJson):
                            if let parsed = ConverterJobOutputParsing.parsePdfOutput(outputJson) {
                                cont.resume(returning: parsed)
                            } else {
                                cont.resume(throwing: UnfurlError.pdfExtractionParseFailed)
                            }
                        case let .failure(error):
                            cont.resume(throwing: error)
                        }
                    }
                } catch {
                    cont.resume(throwing: error)
                }
            }
        }
        defer {
            if let jid = cleanup.jobId {
                Task { @MainActor in
                    _ = try? await runtime.evaluateJavaScriptStringResult(
                        WebBridgeScripts.deletePdfExtractionJobScript(jobId: jid)
                    )
                }
            }
        }
        return result
    }

    @MainActor
    public static func runEpubExtraction(
        runtime: WKWebViewRuntime,
        epubDataUrl: String
    ) async throws -> WebEpubConverterResult {
        guard await HtmlConversionRunner.waitForConverterBridge(runtime: runtime, maxAttempts: 600) else {
            throw UnfurlError.converterBridgeNotReady
        }
        return try await withThrowingTaskGroup(of: WebEpubConverterResult.self) { group in
            group.addTask { @MainActor in
                try await runEpubExtractionInner(runtime: runtime, epubDataUrl: epubDataUrl)
            }
            group.addTask {
                try await Task.sleep(nanoseconds: jobTimeoutNs)
                throw UnfurlError.epubExtractionTimedOut
            }
            let first = try await group.next()!
            group.cancelAll()
            return first
        }
    }

    @MainActor
    private static func runEpubExtractionInner(
        runtime: WKWebViewRuntime,
        epubDataUrl: String
    ) async throws -> WebEpubConverterResult {
        let cleanup = ExtractionJobIdBox()
        let result = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<WebEpubConverterResult, Error>) in
            Task { @MainActor in
                do {
                    let script = WebBridgeScripts.startEpubExtractionScript(resolvedEpubUrl: epubDataUrl)
                    let raw = try await runtime.evaluateJavaScriptStringResult(script)
                    let jobId = WebJsEscaping.decodeJavaScriptStringResult(raw)
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    if jobId.isEmpty {
                        cont.resume(throwing: UnfurlError.epubExtractionStartFailed)
                        return
                    }
                    cleanup.jobId = jobId
                    ConverterJobRegistry.shared.registerEpubJob(jobId: jobId) { res in
                        switch res {
                        case let .success(outputJson):
                            if let parsed = ConverterJobOutputParsing.parseEpubOutput(outputJson) {
                                cont.resume(returning: parsed)
                            } else {
                                cont.resume(throwing: UnfurlError.epubExtractionParseFailed)
                            }
                        case let .failure(error):
                            cont.resume(throwing: error)
                        }
                    }
                } catch {
                    cont.resume(throwing: error)
                }
            }
        }
        defer {
            if let jid = cleanup.jobId {
                Task { @MainActor in
                    _ = try? await runtime.evaluateJavaScriptStringResult(
                        WebBridgeScripts.deleteEpubExtractionJobScript(jobId: jid)
                    )
                }
            }
        }
        return result
    }
}

private final class ExtractionJobIdBox: @unchecked Sendable {
    var jobId: String?
}
