//
//  WebJson.swift
//  YABACore
//

import Foundation

public enum WebJson {
    public static func encodeToString(_ value: some Encodable) throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let data = try encoder.encode(value)
        guard let s = String(data: data, encoding: .utf8) else {
            throw NSError(domain: "YabaWebView", code: 10, userInfo: [NSLocalizedDescriptionKey: "UTF-8 encode failed"])
        }
        return s
    }

    public static func shellLoadResultJson(_ result: WebShellLoadResult) -> String {
        let s = result == .loaded ? "loaded" : "error"
        return "{\"result\":\"\(s)\"}"
    }
}
