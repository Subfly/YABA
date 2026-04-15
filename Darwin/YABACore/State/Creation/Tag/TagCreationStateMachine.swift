//
//  TagCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class TagCreationStateMachine: YabaBaseObservableState<TagCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: TagCreationUIState = TagCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: TagCreationEvent) async {
        switch event {
        case let .onInitWithTag(tagId):
            apply { $0.existingTagId = tagId }
        case let .onSelectNewColor(c):
            apply { $0.colorRole = c }
        case let .onSelectNewIcon(icon):
            apply { $0.icon = icon }
        case let .onChangeLabel(label):
            apply { $0.label = label }
        case .onSave:
            let label = state.label.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !label.isEmpty else { return }
            let colorRaw = state.colorRole.rawValue
            if let id = state.existingTagId {
                TagManager.queueUpdateTagMetadata(tagId: id, label: label, icon: state.icon, colorRaw: colorRaw)
            } else {
                TagManager.queueCreateTag(label: label, icon: state.icon, colorRaw: colorRaw)
            }
        }
    }
}
