//
//  IconCategorySelectionStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class IconCategorySelectionStateMachine: YabaBaseObservableState<IconCategorySelectionUIState>, YabaScreenStateMachine {
    public override init(initialState: IconCategorySelectionUIState = IconCategorySelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: IconCategorySelectionEvent) async {
        if case .onInit = event {
            _ = await IconManager.loadAllCategories()
        }
    }
}
