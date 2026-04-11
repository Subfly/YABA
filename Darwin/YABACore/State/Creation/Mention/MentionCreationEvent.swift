//
//  MentionCreationEvent.swift
//  YABACore
//
//  Parity with Compose `MentionCreationEvent` (creation/mention).
//

import Foundation

public enum MentionCreationEvent: Sendable {
    case onInit(
        initialText: String,
        initialBookmarkId: String?,
        isEdit: Bool,
        editPos: Int?
    )
    case onChangeMentionText(String)
    /// User finished bookmark selection; host may supply `bookmarkLabel` so mention text can auto-fill when still blank.
    case onBookmarkPickedFromSelection(bookmarkId: String, bookmarkLabel: String?)
}
