//
//  YabaDataUrlCodec.swift
//  YABACore
//
//  Data URL helpers for docmark converter input (Compose parity).
//

import Foundation

public enum YabaDataUrlCodec {
    public static func applicationPdfDataUrl(documentBytes: Data) -> String {
        "data:application/pdf;base64,\(documentBytes.base64EncodedString())"
    }

    public static func applicationEpubZipDataUrl(documentBytes: Data) -> String {
        "data:application/epub+zip;base64,\(documentBytes.base64EncodedString())"
    }

    /// Decodes `data:*;base64,...` payloads (e.g. PNG preview from converter).
    public static func decodeBase64DataUrl(_ dataUrl: String?) -> Data? {
        guard let dataUrl = dataUrl?.trimmingCharacters(in: .whitespacesAndNewlines), !dataUrl.isEmpty else {
            return nil
        }
        guard let comma = dataUrl.firstIndex(of: ",") else { return nil }
        let b64 = String(dataUrl[dataUrl.index(after: comma)...])
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if b64.isEmpty { return nil }
        return Data(base64Encoded: b64)
    }
}
