//
//  CollectionItemState.swift
//  YABA
//
//  Created by Ali Taha on 23.10.2025.
//

import SwiftUI

@MainActor
@Observable
internal class CollectionItemState {
    var shouldShowDeleteDialog: Bool = false
    var shouldShowEditSheet: Bool = false
    var shouldShowMenuItems: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
    var shouldShowMoveFolderSheet: Bool = false

    /// Row “new bookmark” flow: type sheet → kind form (`bookmarkCreateTwoStepSheets`).
    var bookmarkTypeSelection: BookmarkTypeSelectionContext?

    var isHovered: Bool = false
    var isTargeted: Bool = false
}
