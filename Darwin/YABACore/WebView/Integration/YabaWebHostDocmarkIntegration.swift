//
//  YabaWebHostDocmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostDocmarkIntegration {
    public static func docmarkEvents(from event: YabaWebHostEvent) -> [DocmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: YabaWebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? YabaWebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        default:
            return []
        }
    }
}
