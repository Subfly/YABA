//
//  TagSelectionEvent.swift
//  YABACore
//
//  Parity with Compose `TagSelectionEvent` — uses tag IDs; labels come from `@Query`.
//

import Foundation

public enum TagSelectionEvent: Sendable {
    case onInit(selectedTagIds: [String])
    case onSearchQueryChanged(String)
    case onSelectTag(tagId: String)
    case onDeselectTag(tagId: String)
}
