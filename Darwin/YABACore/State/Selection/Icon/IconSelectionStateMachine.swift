//
//  IconSelectionStateMachine.swift
//  YABACore
//

import Foundation

@MainActor
public final class IconSelectionStateMachine: YabaBaseObservableState<IconSelectionUIState>, YabaScreenStateMachine {
    public override init(initialState: IconSelectionUIState = IconSelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: IconSelectionEvent) async {
        switch event {
        case let .onInit(category, initialSelectedIcon):
            apply {
                $0.category = category
                $0.selectedIconName = initialSelectedIcon
            }
            _ = await IconManager.loadIconsForCategory(category)
        case let .onSelectIcon(iconName):
            apply { $0.selectedIconName = iconName }
        }
    }
}
