//
//  IconCategorySelectionStateMachine.swift
//  YABACore
//

import Foundation

/// Parity with Compose `IconCategorySelectionStateMachine`.
@MainActor
public final class IconCategorySelectionStateMachine: YabaBaseObservableState<IconCategorySelectionUIState>, YabaScreenStateMachine {
    private var didLoadCategories = false

    public override init(initialState: IconCategorySelectionUIState = IconCategorySelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: IconCategorySelectionEvent) async {
        switch event {
        case .onInit:
            guard !didLoadCategories else { return }
            didLoadCategories = true
            apply { $0.isLoading = true }
            let categories = await IconManager.loadAllCategories()
            apply {
                $0.categories = categories
                $0.isLoading = false
            }
        }
    }
}
