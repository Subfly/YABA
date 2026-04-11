//
//  WebHostNotemarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum WebHostNotemarkIntegration {
    public static func notemarkEvents(from event: WebHostEvent) -> [NotemarkDetailEvent] {
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
        case .noteEditorIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
