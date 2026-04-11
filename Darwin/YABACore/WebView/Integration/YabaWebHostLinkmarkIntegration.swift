//
//  YabaWebHostLinkmarkIntegration.swift
//  YABACore
//
//  Maps `YabaWebHostEvent` into `LinkmarkDetailEvent` for `LinkmarkDetailStateMachine`.
//

import Foundation

@MainActor
public enum YabaWebHostLinkmarkIntegration {
    /// Converts a host event from the web runtime into linkmark detail events (best-effort).
    public static func linkmarkEvents(from event: YabaWebHostEvent) -> [LinkmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onReaderWebInitialContentLoad(resultJson: YabaWebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? YabaWebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        case let .htmlConverterSuccess(result):
            return [.onConverterSucceeded(result)]
        case let .htmlConverterFailure(message):
            return [.onConverterFailed(errorMessage: message)]
        case .readerMetrics:
            return []
        default:
            return []
        }
    }
}
