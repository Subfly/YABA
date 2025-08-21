//
//  KeyboardPadNavigationView.swift
//  YABA
//
//  Created by Ali Taha on 21.08.2025.
//

import SwiftUI
import SwiftData

internal struct KeyboardPadNavigationView: View {
    @EnvironmentObject
    private var layoutState: KeyboardLayoutState
    
    @Query
    private var collections: [YabaCollection]
    
    @State
    private var appState: AppState = .init()
    
    
    var needsInputModeSwitchKey: Bool = false
    var nextKeyboardAction: Selector? = nil
    
    let onClickBookmark: (String) -> Void
    let onAccept: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        viewSwitcher
    }
    
    @ViewBuilder
    private var viewSwitcher: some View {
        if layoutState.isFloating {
            KeyboardMobileNavigationView(
                onClickBookmark: onClickBookmark,
                onAccept: onAccept,
                onDelete: onDelete
            )
        } else {
            NavigationSplitView(columnVisibility: .constant(.doubleColumn)) {
                ZStack {
                    AnimatedGradient(collectionColor: .accentColor)
                    List {
                        HomeCollectionView(
                            collectionType: .folder,
                            collections: collections.filter { $0.collectionType == .folder },
                            onNavigationCallback: { _ in }
                        )
                        HomeCollectionView(
                            collectionType: .tag,
                            collections: collections.filter { $0.collectionType == .tag },
                            onNavigationCallback: { _ in }
                        )
                    }
                    .listStyle(.sidebar)
                    .scrollContentBackground(.hidden)
                }
                .navigationTitle(Text("YABA"))
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        YabaIconView(bundleKey: "internet")
                            .foregroundStyle(.tint)
                            .overlay {
                                if let action = nextKeyboardAction {
                                    NextKeyboardButtonOverlay(action: action)
                                }
                            }
                    }
                }
            } detail: {
                GeneralCollectionDetail(
                    onNavigationCallback: { bookmark in
                        onClickBookmark(bookmark.link)
                    },
                    onAcceptKeyboard: onAccept,
                    onDeleteKeyboard: onDelete
                )
            }
            .navigationSplitViewStyle(.balanced)
            .environment(\.appState, appState)
        }
    }
}
