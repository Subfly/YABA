//
//  YabaWebHostDocmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostDocmarkIntegration {
    public static func docmarkEvents(from event: YabaDarwinWebHostEvent) -> [DocmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: YabaDarwinWebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? YabaDarwinWebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        default:
            return []
        }
    }
}
