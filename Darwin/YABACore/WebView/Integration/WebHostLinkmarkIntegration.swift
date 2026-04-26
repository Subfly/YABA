//
//  WebHostLinkmarkIntegration.swift
//  YABACore
//
//  Maps `WebHostEvent` into `LinkmarkDetailEvent` for `LinkmarkDetailStateMachine`.
//

import Foundation

@MainActor
public enum WebHostLinkmarkIntegration {
    /// Converts a host event from the web runtime into linkmark detail events (best-effort).
    public static func linkmarkEvents(from event: WebHostEvent) -> [LinkmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onReaderWebInitialContentLoad(resultJson: WebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? WebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        case .readerMetrics:
            return []
        default:
            return []
        }
    }
}
