//
//  TagSelectionUIState.swift
//  YABACore
//

import Foundation

public struct TagSelectionUIState: Sendable {
    public var selectedTagIds: Set<String>
    public var searchQuery: String

    public init(selectedTagIds: Set<String> = [], searchQuery: String = "") {
        self.selectedTagIds = selectedTagIds
        self.searchQuery = searchQuery
    }
}
