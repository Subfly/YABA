//
//  NotemarkMentionCreationUIState.swift
//  YABACore
//

import Foundation

public struct NotemarkMentionCreationUIState: Sendable {
    public var mentionText: String
    public var selectedBookmarkId: String?
    /// Resolved from `@Query` or passed from selection; mirrors Compose `BookmarkUiModel` for display.
    public var selectedBookmarkLabel: String?
    public var isEdit: Bool
    public var editPos: Int?

    public init(
        mentionText: String = "",
        selectedBookmarkId: String? = nil,
        selectedBookmarkLabel: String? = nil,
        isEdit: Bool = false,
        editPos: Int? = nil
    ) {
        self.mentionText = mentionText
        self.selectedBookmarkId = selectedBookmarkId
        self.selectedBookmarkLabel = selectedBookmarkLabel
        self.isEdit = isEdit
        self.editPos = editPos
    }
}
