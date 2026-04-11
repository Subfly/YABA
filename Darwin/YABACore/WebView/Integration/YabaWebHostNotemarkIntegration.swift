//
//  YabaWebHostNotemarkIntegration.swift
//  YABACore
//

import Foundation

@MainActor
public enum YabaWebHostNotemarkIntegration {
    public static func notemarkEvents(from event: YabaDarwinWebHostEvent) -> [NotemarkDetailEvent] {
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
        case .noteEditorIdleForAutosave:
            return []
        default:
            return []
        }
    }
}
