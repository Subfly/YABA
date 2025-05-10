//
//  YABANavigationView.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI

struct YabaNavigationView: View {
    @AppStorage(Constants.hasPassedOnboardingKey)
    private var hasPassedOnboarding: Bool = false
    
    var body: some View {
        return navigationSwitcher.tint(.accentColor)
    }
    
    @ViewBuilder
    private var navigationSwitcher: some View {
        #if os(visionOS)
        GenericNavigationView()
        #else
        if UIDevice.current.userInterfaceIdiom == .pad {
            GenericNavigationView()
        } else {
            MobileNavigationView()
        }
        #endif
    }
}

private struct GenericNavigationView: View {
    @Environment(\.appState)
    private var appState
    
    @State
    private var columnVisibility: NavigationSplitViewVisibility = .all
    
    @State
    private var prefferedColumn: NavigationSplitViewColumn = .sidebar
    
    @State
    private var shouldShowSettingsSheet: Bool = false
    
    var body: some View {
        let _ = Self._printChanges()
        NavigationSplitView(
            columnVisibility: $columnVisibility,
            preferredCompactColumn: $prefferedColumn,
        ) {
            HomeView(
                onNavigationCallbackForCollection: { collection in
                    appState.selectedCollection = collection
                },
                onNavigationCallbackForBookmark: { bookmark in
                    appState.selectedBookmark = bookmark
                },
                onNavigationCallbackForSettings: {
                    shouldShowSettingsSheet = true
                }
            )
        } content: {
            CollectionDetail(
                collection: appState.selectedCollection,
                onNavigationCallback: { bookmark in
                    appState.selectedBookmark = bookmark
                }
            )
        } detail: {
            BookmarkDetail(
                bookmark: appState.selectedBookmark,
                onCollectionNavigationCallback: { collection in
                    appState.selectedCollection = collection
                },
                onDeleteBookmarkCallback: { bookmark in
                    if appState.selectedBookmark?.id == bookmark.id {
                        appState.selectedBookmark = nil
                    }
                }
            )
        }
        .navigationSplitViewStyle(.balanced)
        .sheet(isPresented: $shouldShowSettingsSheet) {
            SettingsView()
        }
    }
}

private struct MobileNavigationView: View {
    @State
    private var path: [NavigationDestination] = []
    
    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onNavigationCallbackForCollection: { collection in
                    path.append(.collectionDetail(collection: collection))
                },
                onNavigationCallbackForBookmark: { bookmark in
                    path.append(.bookmarkDetail(bookmark: bookmark))
                },
                onNavigationCallbackForSettings: {
                    path.append(.settings)
                }
            )
            .navigationDestination(for: NavigationDestination.self) { destination in
                switch destination {
                case .collectionDetail(let collection):
                    CollectionDetail(
                        collection: collection,
                        onNavigationCallback: { bookmark in
                            path.append(.bookmarkDetail(bookmark: bookmark))
                        }
                    )
                case .bookmarkDetail(let bookmark):
                    BookmarkDetail(
                        bookmark: bookmark,
                        onCollectionNavigationCallback: { collection in
                            path.append(.collectionDetail(collection: collection))
                        },
                        onDeleteBookmarkCallback: { bookmark in
                            if let firstOccurance = path.firstIndex(
                                of: .bookmarkDetail(bookmark: bookmark)
                            ) {
                                path.removeLast(path.count - firstOccurance)
                            } else {
                                path.removeLast(path.count)
                            }
                        }
                    )
                case .settings:
                    SettingsView()
                }
            }
        }
    }
}

#Preview {
    YabaNavigationView()
}
