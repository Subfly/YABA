//
//  YabaDarwinConverterJobRegistry.swift
//  YABACore
//
//  Parity with Compose `YabaConverterJobBridge.kt` — jobId-keyed completion for HTML/PDF/EPUB.
//

import Foundation

public struct YabaDarwinHtmlConverterResult: Sendable {
    public var documentJson: String
    public var linkMetadataJson: String?

    public init(documentJson: String, linkMetadataJson: String?) {
        self.documentJson = documentJson
        self.linkMetadataJson = linkMetadataJson
    }
}

/// Thread-safe registry for async converter jobs posted via `converterJob` messages.
public final class YabaDarwinConverterJobRegistry: @unchecked Sendable {
    public static let shared = YabaDarwinConverterJobRegistry()

    private let lock = NSLock()
    private var htmlJobs: [String: (Result<YabaDarwinHtmlConverterResult, Error>) -> Void] = [:]
    private var pdfJobs: [String: (Result<String, Error>) -> Void] = [:]
    private var epubJobs: [String: (Result<String, Error>) -> Void] = [:]

    private init() {}

    public func registerHtmlJob(jobId: String, completion: @escaping (Result<YabaDarwinHtmlConverterResult, Error>) -> Void) {
        lock.lock()
        htmlJobs[jobId] = completion
        lock.unlock()
    }

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

    public func removeHtmlJob(jobId: String) {
        lock.lock()
        htmlJobs.removeValue(forKey: jobId)
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
        case "html":
            lock.lock()
            let completion = htmlJobs[jobId]
            lock.unlock()
            guard let completion else { return }
            switch status {
            case "done":
                let outputJson = root["outputJson"] as? String ?? ""
                if let result = Self.parseHtmlOutput(outputJson) {
                    completion(.success(result))
                } else {
                    completion(.failure(NSError(domain: "YabaWebView", code: 1, userInfo: [NSLocalizedDescriptionKey: "HTML parse failed"])))
                }
                lock.lock()
                htmlJobs.removeValue(forKey: jobId)
                lock.unlock()
            case "error":
                let err = root["error"] as? String ?? "HTML conversion failed"
                completion(.failure(NSError(domain: "YabaWebView", code: 2, userInfo: [NSLocalizedDescriptionKey: err])))
                lock.lock()
                htmlJobs.removeValue(forKey: jobId)
                lock.unlock()
            default:
                break
            }
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

    private static func parseHtmlOutput(_ outputJson: String) -> YabaDarwinHtmlConverterResult? {
        guard let data = outputJson.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }
        let documentJson = json["documentJson"] as? String ?? ""
        var linkMetaJson: String?
        if let linkMeta = json["linkMetadata"] {
            if let d = try? JSONSerialization.data(withJSONObject: linkMeta, options: []) {
                linkMetaJson = String(data: d, encoding: .utf8)
            }
        }
        return YabaDarwinHtmlConverterResult(documentJson: documentJson, linkMetadataJson: linkMetaJson)
    }
}

/// Minimal stub for `editorPdfExport` messages — extend when PDF export from editor is wired.
public final class YabaDarwinEditorPdfExportJobRegistry: @unchecked Sendable {
    public static let shared = YabaDarwinEditorPdfExportJobRegistry()
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
            let b64 = root["base64"] as? String ?? ""
            completion(.success(b64))
        case "error":
            let err = root["error"] as? String ?? "PDF export failed"
            completion(.failure(NSError(domain: "YabaWebView", code: 5, userInfo: [NSLocalizedDescriptionKey: err])))
        default:
            break
        }
    }
}
