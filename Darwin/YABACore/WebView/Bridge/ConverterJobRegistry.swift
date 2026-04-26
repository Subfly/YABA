//
//  ConverterJobRegistry.swift
//  YABACore
//
//  Parity with Compose `YabaConverterJobBridge.kt` — jobId-keyed completion for PDF/EPUB `converterJob` messages.
//

import Foundation

/// Thread-safe registry for async converter jobs posted via `converterJob` messages.
public final class ConverterJobRegistry: @unchecked Sendable {
    public static let shared = ConverterJobRegistry()

    private let lock = NSLock()
    private var pdfJobs: [String: (Result<String, Error>) -> Void] = [:]
    private var epubJobs: [String: (Result<String, Error>) -> Void] = [:]

    private init() {}

    public func registerPdfJob(jobId: String, completion: @escaping (Result<String, Error>) -> Void) {
        lock.lock()
        pdfJobs[jobId] = completion
        lock.unlock()
    }

    public func registerEpubJob(jobId: String, completion: @escaping (Result<String, Error>) -> Void) {
        lock.lock()
        epubJobs[jobId] = completion
        lock.unlock()
    }

    public func removePdfJob(jobId: String) {
        lock.lock()
        pdfJobs.removeValue(forKey: jobId)
        lock.unlock()
    }

    public func removeEpubJob(jobId: String) {
        lock.lock()
        epubJobs.removeValue(forKey: jobId)
        lock.unlock()
    }

    func handleConverterJobMessage(_ root: [String: Any]) {
        let jobId = root["jobId"] as? String ?? ""
        if jobId.isEmpty { return }
        let kind = root["kind"] as? String ?? ""
        let status = root["status"] as? String ?? ""

        switch kind {
        case "pdf":
            lock.lock()
            let completion = pdfJobs[jobId]
            lock.unlock()
            guard let completion else { return }
            switch status {
            case "done":
                let outputJson = root["outputJson"] as? String ?? ""
                completion(.success(outputJson))
                lock.lock()
                pdfJobs.removeValue(forKey: jobId)
                lock.unlock()
            case "error":
                let err = root["error"] as? String ?? "PDF extraction failed"
                completion(.failure(NSError(domain: "YabaWebView", code: 3, userInfo: [NSLocalizedDescriptionKey: err])))
                lock.lock()
                pdfJobs.removeValue(forKey: jobId)
                lock.unlock()
            default:
                break
            }
        case "epub":
            lock.lock()
            let completion = epubJobs[jobId]
            lock.unlock()
            guard let completion else { return }
            switch status {
            case "done":
                let outputJson = root["outputJson"] as? String ?? ""
                completion(.success(outputJson))
                lock.lock()
                epubJobs.removeValue(forKey: jobId)
                lock.unlock()
            case "error":
                let err = root["error"] as? String ?? "EPUB extraction failed"
                completion(.failure(NSError(domain: "YabaWebView", code: 4, userInfo: [NSLocalizedDescriptionKey: err])))
                lock.lock()
                epubJobs.removeValue(forKey: jobId)
                lock.unlock()
            default:
                break
            }
        default:
            break
        }
    }
}

/// Minimal stub for `editorPdfExport` messages — extend when PDF export from editor is wired.
public final class EditorPdfExportJobRegistry: @unchecked Sendable {
    public static let shared = EditorPdfExportJobRegistry()
    private let lock = NSLock()
    private var jobs: [String: (Result<String, Error>) -> Void] = [:]

    private init() {}

    public func register(jobId: String, completion: @escaping (Result<String, Error>) -> Void) {
        lock.lock()
        jobs[jobId] = completion
        lock.unlock()
    }

    func handleMessage(_ root: [String: Any]) {
        let jobId = root["jobId"] as? String ?? ""
        if jobId.isEmpty { return }
        let status = root["status"] as? String ?? ""
        lock.lock()
        let completion = jobs.removeValue(forKey: jobId)
        lock.unlock()
        guard let completion else { return }
        switch status {
        case "done":
            let b64 = (root["pdfBase64"] as? String)
                ?? (root["base64"] as? String)
                ?? ""
            completion(.success(b64))
        case "error":
            let err = root["error"] as? String ?? "PDF export failed"
            completion(.failure(NSError(domain: "YabaWebView", code: 5, userInfo: [NSLocalizedDescriptionKey: err])))
        default:
            break
        }
    }
}
