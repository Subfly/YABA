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
    private var path: NavigationPath = .init()
    
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
                onNavigationCallbackForBookmark: { _  in },
            )
        } content: {
            CollectionDetail(
                collection: $selectedCollection,
                selectedBookmark: $selectedBookmark,
                onNavigationCallback: { _ in }
            )
        } detail: {
            BookmarkDetail(
                selectedCollection: $selectedCollection,
                bookmark: $selectedBookmark,
                onCollectionNavigationCallback: { _ in },
                onDeleteBookmarkCallback: {
                    selectedBookmark = nil
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
    }
    
    @ViewBuilder
    private var mobileNavigationView: some View {
        NavigationStack(path: $path) {
            HomeView(
                selectedCollection: $selectedCollection,
                selectedBookmark: $selectedBookmark,
                selectedAppTint: $appTint,
                onNavigationCallbackForCollection: { collection in
                    path.append(collection)
                },
                onNavigationCallbackForBookmark: { bookmark in
                    path.append(bookmark)
                }
            )
            .navigationDestination(for: YabaCollection.self) { collection in
                CollectionDetail(
                    collection: .init(
                        get: { collection },
                        set: { _ in }
                    ),
                    selectedBookmark: $selectedBookmark,
                    onNavigationCallback: { bookmark in
                        path.append(bookmark)
                    }
                )
            }
            .navigationDestination(for: Bookmark.self) { bookmark in
                BookmarkDetail(
                    selectedCollection: $selectedCollection,
                    bookmark: .init(
                        get: { bookmark },
                        set: { _ in }
                    ),
                    onCollectionNavigationCallback: { collection in
                        path.append(collection)
                    },
                    onDeleteBookmarkCallback: {
                        path.removeLast(path.count)
                    }
                )
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
