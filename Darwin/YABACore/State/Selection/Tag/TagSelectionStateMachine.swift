//
//  TagSelectionStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class TagSelectionStateMachine: YabaBaseObservableState<TagSelectionUIState>, YabaScreenStateMachine {
    public override init(initialState: TagSelectionUIState = TagSelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: TagSelectionEvent) async {
        switch event {
        case let .onInit(selectedTagIds):
            apply { $0.selectedTagIds = Set(selectedTagIds) }
        case let .onSearchQueryChanged(q):
            apply { $0.searchQuery = q }
        case let .onSelectTag(tagId):
            apply { $0.selectedTagIds.insert(tagId) }
        case let .onDeselectTag(tagId):
            apply { $0.selectedTagIds.remove(tagId) }
        }
    }
}
