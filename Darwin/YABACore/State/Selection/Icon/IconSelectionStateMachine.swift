//
//  IconSelectionStateMachine.swift
//  YABACore
//

import Foundation

/// Parity with Compose `IconSelectionStateMachine`: loads icons for a category, caches by category id,
/// and tracks selection (no `ResultStore`; callers commit via bindings or explicit callbacks).
@MainActor
public final class IconSelectionStateMachine: YabaBaseObservableState<IconSelectionUIState>, YabaScreenStateMachine {
    private var loadIconsTask: Task<Void, Never>?
    private var iconsByCategoryId: [String: [YabaIconItem]] = [:]

    public override init(initialState: IconSelectionUIState = IconSelectionUIState()) {
        super.init(initialState: initialState)
    }

    public func send(_ event: IconSelectionEvent) async {
        switch event {
        case let .onInit(category, initialSelectedIcon):
            loadIconsTask?.cancel()
            apply {
                $0.selectedIconName = initialSelectedIcon
                $0.isLoadingIcons = true
                $0.icons = []
            }
            if let cached = iconsByCategoryId[category.id] {
                apply {
                    $0.icons = cached
                    $0.isLoadingIcons = false
                }
                return
            }
            let captured = category
            loadIconsTask = Task { @MainActor [weak self] in
                guard let self else { return }
                let icons = await IconManager.loadIconsForCategory(captured)
                guard !Task.isCancelled else {
                    self.apply {
                        if $0.isLoadingIcons {
                            $0.isLoadingIcons = false
                        }
                    }
                    return
                }
                self.iconsByCategoryId[captured.id] = icons
                self.apply {
                    $0.icons = icons
                    $0.isLoadingIcons = false
                }
            }
        case let .onSelectIcon(iconName):
            apply { $0.selectedIconName = iconName }
        }
    }
}
