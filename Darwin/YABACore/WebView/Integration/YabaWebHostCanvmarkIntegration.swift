//
//  YabaWebHostCanvmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostCanvmarkIntegration {
    public static func canvmarkEvents(from event: YabaWebHostEvent) -> [CanvmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: YabaWebJson.shellLoadResultJson(result))]
        case let .canvasMetrics(metrics):
            guard let json = try? YabaWebJson.encodeToString(metrics) else { return [] }
            return [.onCanvasMetricsChanged(metricsJson: json)]
        case let .canvasStyleState(style):
            guard let json = try? YabaWebJson.encodeToString(style) else { return [] }
            return [.onCanvasStyleStateChanged(styleJson: json)]
        case .canvasIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
