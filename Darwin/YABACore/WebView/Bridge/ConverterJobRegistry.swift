//
//  ConverterJobRegistry.swift
//  YABACore
//
//  Parity with Compose `YabaConverterJobBridge.kt` — jobId-keyed completion for HTML/PDF/EPUB.
//

import Foundation

public struct HtmlConverterResult: Sendable {
    public var documentJson: String
    public var linkMetadataJson: String?
    /// When parsed from `outputJson`, includes asset descriptors for reader image download.
    public var assets: [WebConverterAsset]

    public init(documentJson: String, linkMetadataJson: String?, assets: [WebConverterAsset] = []) {
        self.documentJson = documentJson
        self.linkMetadataJson = linkMetadataJson
        self.assets = assets
    }

    /// Structured result for [ConverterResultProcessor] and link metadata updates.
    public func asWebConverterResult() -> WebConverterResult {
        let meta: WebLinkMetadata
        if let linkMetadataJson,
           let data = linkMetadataJson.data(using: .utf8),
           let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            meta = WebLinkMetadata(
                cleanedUrl: (obj["cleanedUrl"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
                title: (obj["title"] as? String)?.nilIfEmpty,
                description: (obj["description"] as? String)?.nilIfEmpty,
                author: (obj["author"] as? String)?.nilIfEmpty,
                date: (obj["date"] as? String)?.nilIfEmpty,
                audio: (obj["audio"] as? String)?.nilIfEmpty,
                video: (obj["video"] as? String)?.nilIfEmpty,
                image: (obj["image"] as? String)?.nilIfEmpty,
                logo: (obj["logo"] as? String)?.nilIfEmpty
            )
        } else {
            meta = WebLinkMetadata(cleanedUrl: "")
        }
        return WebConverterResult(
            documentJson: documentJson,
            assets: assets,
            linkMetadata: meta
        )
    }
}

/// Thread-safe registry for async converter jobs posted via `converterJob` messages.
public final class ConverterJobRegistry: @unchecked Sendable {
    public static let shared = ConverterJobRegistry()

    private let lock = NSLock()
    private var htmlJobs: [String: (Result<HtmlConverterResult, Error>) -> Void] = [:]
    private var pdfJobs: [String: (Result<String, Error>) -> Void] = [:]
    private var epubJobs: [String: (Result<String, Error>) -> Void] = [:]

    private init() {}

    public func registerHtmlJob(jobId: String, completion: @escaping (Result<HtmlConverterResult, Error>) -> Void) {
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

    private static func parseHtmlOutput(_ outputJson: String) -> HtmlConverterResult? {
        guard let data = outputJson.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }
        let documentJson = (json["documentJson"] as? String) ?? (json["markdown"] as? String) ?? ""
        var linkMetaJson: String?
        if let linkMeta = json["linkMetadata"] {
            if let d = try? JSONSerialization.data(withJSONObject: linkMeta, options: []) {
                linkMetaJson = String(data: d, encoding: .utf8)
            }
        }
        var assets: [WebConverterAsset] = []
        if let assetsArr = json["assets"] as? [[String: Any]] {
            for item in assetsArr {
                let placeholder = item["placeholder"] as? String ?? ""
                let url = item["url"] as? String ?? ""
                let altRaw = item["alt"] as? String
                let alt = altRaw?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                if !placeholder.isEmpty, !url.isEmpty {
                    assets.append(WebConverterAsset(placeholder: placeholder, url: url, alt: alt))
                }
            }
        }
        return HtmlConverterResult(documentJson: documentJson, linkMetadataJson: linkMetaJson, assets: assets)
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
