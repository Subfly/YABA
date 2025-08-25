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
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var shouldShowOnboardingView: Bool = false
    
    var body: some View {
        navigationSwitcher
            .fullScreenCover(isPresented: $shouldShowOnboardingView) {
                OnboardingView {
                    hasPassedOnboarding = true
                }
            }
            .onAppear {
                YabaDataLogger.shared.setContext(modelContext)
                if !hasPassedOnboarding {
                    shouldShowOnboardingView = true
                }
            }
            .onChange(of: hasPassedOnboarding) { _, passed in
                if passed {
                    shouldShowOnboardingView = false
                }
            }
    }
    
    @ViewBuilder
    private var navigationSwitcher: some View {
        #if os(visionOS)
        GenericNavigationView()
        #elseif targetEnvironment(macCatalyst)
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
    @State
    private var columnVisibility: NavigationSplitViewVisibility = .all
    
    @State
    private var prefferedColumn: NavigationSplitViewColumn = .sidebar
    
    @State
    private var shouldShowSearch: Bool = false
    
    @State
    private var shouldShowSettingsSheet: Bool = false
    
    var body: some View {
        NavigationSplitView(
            columnVisibility: $columnVisibility,
            preferredCompactColumn: $prefferedColumn,
        ) {
            HomeView(
                onNavigationCallbackForCollection: { _ in },
                onNavigationCallbackForBookmark: { _ in },
                onNavigationCallbackForSearch: {
                    withAnimation {
                        shouldShowSearch.toggle()
                    }
                },
                onNavigationCallbackForSettings: {
                    shouldShowSettingsSheet = true
                }
            )
        } content: {
            if shouldShowSearch {
                SearchView(
                    onCloseRequested: {
                        withAnimation {
                            shouldShowSearch = false
                        }
                    },
                    onSelectBookmark: { _ in }
                )
            } else {
                GeneralCollectionDetail(
                    onNavigationCallback: { _ in },
                    onAcceptKeyboard: {},
                    onDeleteKeyboard: {}
                )
            }
        } detail: {
            GeneralBookmarkDetail(
                onCollectionNavigationCallback: { _ in },
                onDeleteBookmarkCallback: { _ in }
            )
        }
        .navigationSplitViewStyle(.balanced)
        .sheet(isPresented: $shouldShowSettingsSheet) {
            SettingsView()
        }
    }
}

private struct MobileNavigationView: View {
    @Environment(\.deepLinkManager)
    private var deepLinkManager
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var path: [NavigationDestination] = []
    
    @State
    private var shouldShowSettings: Bool = false
    
    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onNavigationCallbackForCollection: { collection in
                    path.append(.collectionDetail(collection: collection))
                },
                onNavigationCallbackForBookmark: { bookmark in
                    path.append(.bookmarkDetail(bookmark: bookmark))
                },
                onNavigationCallbackForSearch: {
                    path.append(.search)
                },
                onNavigationCallbackForSettings: {
                    shouldShowSettings = true
                }
            )
            .navigationDestination(for: NavigationDestination.self) { destination in
                switch destination {
                case .collectionDetail(let collection):
                    MobileCollectionDetail(
                        collection: collection,
                        onNavigationCallback: { bookmark in
                            path.append(.bookmarkDetail(bookmark: bookmark))
                        },
                        onAcceptKeyboard: {},
                        onDeleteKeyboard: {}
                    )
                    .navigationBarBackButtonHidden()
                case .bookmarkDetail(let bookmark):
                    MobileBookmarkDetail(
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
                    .navigationBarBackButtonHidden()
                case .search:
                    SearchView(
                        onCloseRequested: {},
                        onSelectBookmark: { bookmark in
                            path.append(.bookmarkDetail(bookmark: bookmark))
                        }
                    )
                    .navigationBarBackButtonHidden()
                }
            }
        }
        .fullScreenCover(isPresented: $shouldShowSettings) {
            SettingsView()
                .navigationBarBackButtonHidden()
                .interactiveDismissDisabled()
        }
        .onChange(of: deepLinkManager.openBookmarkRequest) { oldValue, newValue in
            if oldValue == nil {
                if let newRequest = newValue {
                    let id = newRequest.bookmarkId
                    if let bookmarks = try? modelContext.fetch(
                        .init(predicate: #Predicate<YabaBookmark> { $0.bookmarkId ==  id })
                    ), let bookmark = bookmarks.first {
                        path.append(.bookmarkDetail(bookmark: bookmark))
                    }
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
        .onChange(of: deepLinkManager.openCollectionRequest) { oldValue, newValue in
            if oldValue == nil {
                if let newRequest = newValue {
                    let id = newRequest.collectionId
                    if let collections = try? modelContext.fetch(
                        .init(predicate: #Predicate<YabaCollection> { $0.collectionId ==  id })
                    ), let collection = collections.first {
                        path.append(.collectionDetail(collection: collection))
                    }
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
    }
}

#Preview {
    YabaNavigationView()
}
