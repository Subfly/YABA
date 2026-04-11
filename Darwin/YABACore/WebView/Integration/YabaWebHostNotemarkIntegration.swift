//
//  YabaWebHostNotemarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostNotemarkIntegration {
    public static func notemarkEvents(from event: YabaWebHostEvent) -> [NotemarkDetailEvent] {
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
        case .noteEditorIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
