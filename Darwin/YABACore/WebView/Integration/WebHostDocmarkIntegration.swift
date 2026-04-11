//
//  WebHostDocmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum WebHostDocmarkIntegration {
    public static func docmarkEvents(from event: WebHostEvent) -> [DocmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: WebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? WebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        default:
            return []
        }
    }
}
