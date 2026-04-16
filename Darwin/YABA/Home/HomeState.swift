//
//  HomeState.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

@Observable
@MainActor
internal class HomeState {
    var isFABActive: Bool = false
    
    var shouldShowBackground: Bool = false
    var shouldShowCreateFolderSheet: Bool = false
    var shouldShowCreateTagSheet: Bool = false
    // var shouldShowSyncSheet: Bool = false

    /// Presents `BookmarkFlowSheet` for edit (`BookmarkItemView`) or deep-link link capture.
    var bookmarkFlow: BookmarkFlowContext?

    /// FAB bookmark create: type sheet → kind form (`bookmarkCreateTwoStepSheets` on `HomeView`).
    var bookmarkTypeSelection: BookmarkTypeSelectionContext?
}
