//
//  YabaWebHostCanvmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostCanvmarkIntegration {
    public static func canvmarkEvents(from event: YabaDarwinWebHostEvent) -> [CanvmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: YabaDarwinWebJson.shellLoadResultJson(result))]
        case let .canvasMetrics(metrics):
            guard let json = try? YabaDarwinWebJson.encodeToString(metrics) else { return [] }
            return [.onCanvasMetricsChanged(metricsJson: json)]
        case let .canvasStyleState(style):
            guard let json = try? YabaDarwinWebJson.encodeToString(style) else { return [] }
            return [.onCanvasStyleStateChanged(styleJson: json)]
        case .canvasIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
