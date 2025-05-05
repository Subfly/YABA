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
    
    @State
    private var path: [NavigationDestination] = []
    
    @State
    private var appTint: Color = .accentColor
    
    @State
    private var selectedCollection: YabaCollection?
    
    @State
    private var selectedBookmark: Bookmark?
    
    @State
    private var columnVisibility: NavigationSplitViewVisibility = .all
    
    @State
    private var prefferedColumn: NavigationSplitViewColumn = .sidebar
    
    @State
    private var shouldShowSettingsSheet: Bool = false
    
    var body: some View {
        #if os(visionOS)
        genericNavigationView
        #else
        if UIDevice.current.userInterfaceIdiom == .pad {
            genericNavigationView
        } else {
            mobileNavigationView
        }
        #endif
    }
    
    @ViewBuilder
    private var genericNavigationView: some View {
        NavigationSplitView(
            columnVisibility: $columnVisibility,
            preferredCompactColumn: $prefferedColumn,
        ) {
            HomeView(
                selectedCollection: $selectedCollection,
                selectedBookmark: $selectedBookmark,
                selectedAppTint: $appTint,
                onNavigationCallbackForCollection: { _  in },
                onNavigationCallbackForBookmark: { bookmark in
                    selectedBookmark = bookmark
                },
                onNavigationCallbackForSettings: {
                    shouldShowSettingsSheet = true
                }
            )
        } content: {
            CollectionDetail(
                collection: $selectedCollection,
                selectedBookmark: $selectedBookmark,
                onNavigationCallback: { bookmark in
                    if selectedBookmark?.id == bookmark.id {
                        selectedBookmark = nil
                    }
                }
            )
        } detail: {
            BookmarkDetail(
                selectedCollection: $selectedCollection,
                bookmark: $selectedBookmark,
                onCollectionNavigationCallback: { _ in },
                onDeleteBookmarkCallback: { bookmark in
                    if selectedBookmark?.id == bookmark.id {
                        selectedBookmark = nil
                    }
                }
            )
        }
        .navigationSplitViewStyle(.balanced)
        .tint(appTint)
        .onChange(of: selectedCollection) { _, newValue in
            if let color = newValue?.color.getUIColor() {
                withAnimation {
                    appTint = color
                }
            }
        }
        .sheet(isPresented: $shouldShowSettingsSheet) {
            SettingsView()
        }
    }
    
    @ViewBuilder
    private var mobileNavigationView: some View {
        NavigationStack(path: $path) {
            HomeView(
                selectedCollection: $selectedCollection,
                selectedBookmark: $selectedBookmark,
                selectedAppTint: $appTint,
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
                        collection: .init(
                            get: { collection },
                            set: { _ in }
                        ),
                        selectedBookmark: $selectedBookmark,
                        onNavigationCallback: { bookmark in
                            path.append(.bookmarkDetail(bookmark: bookmark))
                        }
                    )
                case .bookmarkDetail(let bookmark):
                    BookmarkDetail(
                        selectedCollection: $selectedCollection,
                        bookmark: .init(
                            get: { bookmark },
                            set: { _ in }
                        ),
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
        .tint(appTint)
        .onChange(of: selectedCollection) { _, newValue in
            if let color = newValue?.color.getUIColor() {
                withAnimation {
                    appTint = color
                }
            }
        }
    }
}

#Preview {
    YabaNavigationView()
}
