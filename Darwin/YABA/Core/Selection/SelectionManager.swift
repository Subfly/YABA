//
//  DragDropManager.swift
//  YABA
//
//  Created by Ali Taha on 14.09.2025.
//

import Foundation
import SwiftUI
import SwiftData

/**
 * YABA does not support moving of tags. Theorettically it is possible,
 * but it should be prevented in every function in here to prevent
 * unexpected behavior.
 *
 * As of a SwiftUI bug, I couldn't be able to implement proper drag
 * drop functionality inside of the ListView. Hence, came up with
 * this, so that users can select items and move them with
 * a Select&Select manner.
 */
@MainActor
@Observable
@preconcurrency
class SelectionManager {
    private let modelContext: ModelContext = YabaModelContainer.getContext()
    
    var isBookmarkSelectionActive: Bool = false
    var selectedBookmarkIds: [YabaBookmark] = []
}

extension EnvironmentValues {
    @Entry var selectionManager: SelectionManager = .init()
}
