//
//  HomeView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

struct HomeView: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @Environment(\.deepLinkManager)
    private var deepLinkManager
    
    @State
    private var homeState: HomeState = .init()
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (YabaBookmark) -> Void
    let onNavigationCallbackForSearch: () -> Void
    let onNavigationCallbackForSettings: () -> Void
    
    var body: some View {
        ZStack {
            #if !targetEnvironment(macCatalyst)
            AnimatedGradient(collectionColor: .accentColor)
            #endif
            SequentialView(
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                onNavigationCallbackForCollection: onNavigationCallbackForCollection,
                onNavigationCallbackForBookmark: onNavigationCallbackForBookmark
            )
            .scrollContentBackground(.hidden)
            #if targetEnvironment(macCatalyst)
            .listRowSpacing(2)
            .listStyle(.sidebar)
            #endif
            
            HomeCreateContentFAB(
                isActive: $homeState.isFABActive,
                onClickAction: { creationType in
                    withAnimation {
                        homeState.isFABActive.toggle()
                        switch creationType {
                        case .tag:
                            homeState.selectedContentCreationType = .tag
                            homeState.shouldShowCreateContentSheet = true
                        case .folder:
                            homeState.selectedContentCreationType = .folder
                            homeState.shouldShowCreateContentSheet = true
                        case .bookmark:
                            homeState.shouldShowCreateBookmarkSheet = true
                        default:
                            break
                        }
                    }
                }
            )
            .transition(.blurReplace)
            .ignoresSafeArea()
        }
        .sheet(isPresented: $homeState.shouldShowCreateContentSheet) {
            if let creationType = homeState.selectedContentCreationType {
                CollectionCreationContent(
                    collectionType: creationType,
                    collectionToEdit: nil,
                    onEditCallback: { _ in }
                )
            }
        }
        .sheet(isPresented: $homeState.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: nil,
                collectionToFill: nil,
                link: nil,
                onExitRequested: {}
            )
        }
        .sheet(item: $homeState.saveBookmarkRequest) { request in
            BookmarkCreationContent(
                bookmarkToEdit: nil,
                collectionToFill: nil,
                link: request.link,
                onExitRequested: {}
            )
        }
        #if !targetEnvironment(macCatalyst)
        .fullScreenCover(isPresented: $homeState.shouldShowSyncSheet) {
            SyncView().interactiveDismissDisabled()
        }
        #else
        .sheet(isPresented: $homeState.shouldShowSyncSheet) {
            SyncView().interactiveDismissDisabled()
        }
        #endif
        .navigationTitle("YABA")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                #if targetEnvironment(macCatalyst)
                MacOSHoverableToolbarIcon(
                    bundleKey: "laptop-phone-sync",
                    tooltipKey: "Synchronize Label",
                    onPressed: {
                        homeState.shouldShowSyncSheet = true
                    }
                )
                #else
                Button {
                    homeState.shouldShowSyncSheet = true
                } label: {
                    YabaIconView(bundleKey: "laptop-phone-sync")
                }
                #endif
            }
            ToolbarItem(placement: .topBarTrailing) {
                #if targetEnvironment(macCatalyst)
                MacOSHoverableToolbarIcon(
                    bundleKey: "search-01",
                    tooltipKey: "Search Title",
                    onPressed: onNavigationCallbackForSearch
                )
                #else
                Button {
                    onNavigationCallbackForSearch()
                } label: {
                    YabaIconView(bundleKey: "search-01")
                }
                #endif
            }
            ToolbarItem(placement: .topBarTrailing) {
                #if targetEnvironment(macCatalyst)
                Menu {
                    ContentAppearancePicker()
                    SortingPicker()
                    Button {
                        onNavigationCallbackForSettings()
                    } label: {
                        Label {
                            Text("Settings Title")
                        } icon: {
                            YabaIconView(bundleKey: "settings-02")
                        }
                    }
                } label: {
                    MacOSHoverableToolbarIcon(
                        bundleKey:  "more-horizontal-circle-02",
                        tooltipKey: "Show More Label",
                        onPressed: {}
                    )
                }
                #else
                Menu {
                    ContentAppearancePicker()
                    SortingPicker()
                    Divider()
                    Button {
                        onNavigationCallbackForSettings()
                    } label: {
                        Label {
                            Text("Settings Title")
                        } icon: {
                            YabaIconView(bundleKey: "settings-02")
                        }
                    }
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                }
                #endif
            }
        }
        .onChange(of: deepLinkManager.saveRequest) { oldValue, newValue in
            if oldValue == nil {
                if let newRequest = newValue {
                    homeState.saveBookmarkRequest = newRequest
                    deepLinkManager.onHandleDeeplink()
                }
            }
        }
    }
}

private struct SequentialView: View {
    @AppStorage(Constants.showRecentsKey)
    private var showRecents: Bool = true
    
    @Query
    private var collections: [YabaCollection]
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (YabaBookmark) -> Void
    
    init(
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onNavigationCallbackForCollection: @escaping (YabaCollection) -> Void,
        onNavigationCallbackForBookmark: @escaping (YabaBookmark) -> Void
    ) {
        self.onNavigationCallbackForCollection = onNavigationCallbackForCollection
        self.onNavigationCallbackForBookmark = onNavigationCallbackForBookmark
        
        let sortDescriptor: SortDescriptor<YabaCollection> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
        }
        
        _collections = Query(
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
        
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                Spacer().frame(height: 24)
                HomeAnnouncementsView()
                if showRecents && UIDevice.current.userInterfaceIdiom == .phone {
                    Spacer().frame(height: 24)
                    HomeRecentsView(
                        onNavigationCallback: onNavigationCallbackForBookmark
                    )
                }
                Spacer().frame(height: 24)
                HomeCollectionView(
                    collectionType: .folder,
                    collections: collections.filter { $0.collectionType == .folder },
                    onNavigationCallback: onNavigationCallbackForCollection
                )
                Spacer().frame(height: 24)
                HomeCollectionView(
                    collectionType: .tag,
                    collections: collections.filter { $0.collectionType == .tag },
                    onNavigationCallback: onNavigationCallbackForCollection
                )
                Spacer().frame(height: 100)
            }
        }
    }
}
