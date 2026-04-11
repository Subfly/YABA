//
//  MentionCreationStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class MentionCreationStateMachine: YabaBaseObservableState<MentionCreationUIState>, YabaScreenStateMachine {
    public override init(initialState: MentionCreationUIState = MentionCreationUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: MentionCreationEvent) async {
        switch event {
        case let .onInit(text, bookmarkId, isEdit, editPos):
            apply {
                $0.mentionText = text
                $0.selectedBookmarkId = bookmarkId
                $0.selectedBookmarkLabel = nil
                $0.isEdit = isEdit
                $0.editPos = editPos
            }
        case let .onChangeMentionText(text):
            apply { $0.mentionText = text }
        case let .onBookmarkPickedFromSelection(bookmarkId, bookmarkLabel):
            apply {
                $0.selectedBookmarkId = bookmarkId
                if let label = bookmarkLabel, $0.mentionText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    $0.mentionText = label
                }
                $0.selectedBookmarkLabel = bookmarkLabel
            }
        }
    }
}
