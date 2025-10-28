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
    
    @Query(
        filter: #Predicate<YabaCollection> { collection in
            collection.parent == nil
        }
    )
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
            ContentUnavailableView {
                Label {
                    Text("Keyboard Not Supported Title")
                } icon: {
                    YabaIconView(bundleKey: "keyboard")
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                }
            } description: {
                Text("Keyboard Not Supported Message")
            } actions: {
                YabaIconView(bundleKey: "internet")
                    .frame(width: 28, height: 28)
                    .foregroundStyle(.tint)
                    .overlay {
                        if let action = nextKeyboardAction {
                            NextKeyboardButtonOverlay(action: action)
                        }
                    }
            }
        } else {
            NavigationSplitView(columnVisibility: .constant(.doubleColumn)) {
                ZStack {
                    AnimatedGradient(collectionColor: .accentColor)
                    ScrollView {
                        LazyVStack {
                            Spacer().frame(height: 24)
                            HomeCollectionView(
                                collectionType: .folder,
                                collections: collections.filter { $0.collectionType == .folder },
                                onNavigationCallback: { _ in }
                            )
                            Spacer().frame(height: 24)
                            HomeCollectionView(
                                collectionType: .tag,
                                collections: collections.filter { $0.collectionType == .tag },
                                onNavigationCallback: { _ in }
                            )
                            Spacer().frame(height: 24)
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
                .navigationTitle(Text("YABA"))
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        if #available(iOS 26, *) {
                            YabaIconView(bundleKey: "internet")
                                .frame(width: 24, height: 24)
                                .foregroundStyle(.white)
                                .glassEffect()
                                .overlay {
                                    if let action = nextKeyboardAction {
                                        NextKeyboardButtonOverlay(action: action)
                                    }
                                }
                        } else {
                            YabaIconView(bundleKey: "internet")
                                .frame(width: 24, height: 24)
                                .foregroundStyle(.tint)
                                .overlay {
                                    if let action = nextKeyboardAction {
                                        NextKeyboardButtonOverlay(action: action)
                                    }
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
