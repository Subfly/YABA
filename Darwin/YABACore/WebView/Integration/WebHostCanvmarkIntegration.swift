//
//  WebHostCanvmarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum WebHostCanvmarkIntegration {
    public static func canvmarkEvents(from event: WebHostEvent) -> [CanvmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onWebInitialContentLoad(resultJson: WebJson.shellLoadResultJson(result))]
        case let .canvasMetrics(metrics):
            guard let json = try? WebJson.encodeToString(metrics) else { return [] }
            return [.onCanvasMetricsChanged(metricsJson: json)]
        case let .canvasStyleState(style):
            guard let json = try? WebJson.encodeToString(style) else { return [] }
            return [.onCanvasStyleStateChanged(styleJson: json)]
        case .canvasIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
