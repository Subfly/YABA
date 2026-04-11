//
//  YabaWebHostLinkmarkIntegration.swift
//  YABACore
//
//  Maps `YabaDarwinWebHostEvent` into `LinkmarkDetailEvent` for `LinkmarkDetailStateMachine`.
//

import Foundation

@MainActor
public enum YabaWebHostLinkmarkIntegration {
    /// Converts a host event from the web runtime into linkmark detail events (best-effort).
    public static func linkmarkEvents(from event: YabaDarwinWebHostEvent) -> [LinkmarkDetailEvent] {
        switch event {
        case let .initialContentLoad(result):
            return [.onReaderWebInitialContentLoad(resultJson: YabaDarwinWebJson.shellLoadResultJson(result))]
        case let .tableOfContentsChanged(toc):
            let json: String?
            if let unwrapped = toc {
                json = try? YabaDarwinWebJson.encodeToString(unwrapped)
            } else {
                json = nil
            }
            return [.onTocChanged(tocJson: json)]
        case let .htmlConverterSuccess(documentJson, linkMetadataJson):
            return [.onConverterSucceeded(documentJson: documentJson, linkMetadataJson: linkMetadataJson)]
        case let .htmlConverterFailure(message):
            return [.onConverterFailed(errorMessage: message)]
        case .readerMetrics:
            return []
        default:
            return []
        }
    }
}
