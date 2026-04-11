//
//  UnfurlHttpClient.swift
//  YABACore
//
//  Parity with Compose `UnfurlHttpClient` (Ktor) — URLSession for HTML and asset bytes.
//

import Foundation

public enum UnfurlHttpClient {
    private static let session: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        return URLSession(configuration: config)
    }()

    /// Fetches page HTML with the same header profile as Compose `Unfurler.loadURL`.
    public static func getHtmlString(url: URL) async throws -> String {
        var request = URLRequest(url: url)
        request.setValue("WhatsApp/2", forHTTPHeaderField: "User-Agent")
        request.setValue("https://google.com/", forHTTPHeaderField: "Referer")
        request.setValue("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", forHTTPHeaderField: "Accept")
        request.setValue("en-US,en;q=0.9", forHTTPHeaderField: "Accept-Language")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200 ..< 300).contains(http.statusCode) else {
            throw UnfurlError.unableToFetchHtml
        }
        return String(data: data, encoding: .utf8) ?? ""
    }

    /// Downloads remote bytes for reader assets or preview images.
    public static func getBytes(url: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.setValue("WhatsApp/2", forHTTPHeaderField: "User-Agent")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200 ..< 300).contains(http.statusCode) else {
            throw UnfurlError.unableToFetchHtml
        }
        return data
    }
}
