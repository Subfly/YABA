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
            // TODO: CHANGE THIS WHEN LAZYVGRID IS RECYCABLE
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
        /*
        #if !targetEnvironment(macCatalyst)
        .fullScreenCover(isPresented: $homeState.shouldShowSyncSheet) {
            SyncView().interactiveDismissDisabled()
        }
        #else
        .sheet(isPresented: $homeState.shouldShowSyncSheet) {
            SyncView().interactiveDismissDisabled()
        }
        #endif
         */
        .navigationTitle("YABA")
        .toolbar {
            ToolbarIcons(
                onNavigationCallbackForSearch: onNavigationCallbackForSearch,
                onNavigationCallbackForSync: { homeState.shouldShowSyncSheet = true },
                onNavigationCallbackForSettings: onNavigationCallbackForSettings
            )
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
        List {
            if UIDevice.current.userInterfaceIdiom == .phone {
                HomeRecentsView(
                    onNavigationCallback: onNavigationCallbackForBookmark
                )
            }
            HomeCollectionView(
                collectionType: .folder,
                collections: collections.filter { $0.collectionType == .folder },
                onNavigationCallback: onNavigationCallbackForCollection
            )
            HomeCollectionView(
                collectionType: .tag,
                collections: collections.filter { $0.collectionType == .tag },
                onNavigationCallback: onNavigationCallbackForCollection
            )
            Spacer().frame(
                height: UIDevice.current.userInterfaceIdiom == .pad
                ? 100
                : 0
            ).listRowBackground(Color.clear)
        }
    }
}

private struct GridView: View {
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @Query
    private var collections: [YabaCollection]
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (YabaBookmark) -> Void
    
    init(
        onNavigationCallbackForCollection: @escaping (YabaCollection) -> Void,
        onNavigationCallbackForBookmark: @escaping (YabaBookmark) -> Void
    ) {
        self.onNavigationCallbackForCollection = onNavigationCallbackForCollection
        self.onNavigationCallbackForBookmark = onNavigationCallbackForBookmark
        
        let sortDescriptor: SortDescriptor<YabaCollection> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredSortOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredSortOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredSortOrder == .ascending ? .forward : .reverse)
        }
        
        _collections = Query(
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
    
    var body: some View {
        ScrollView {
            LazyVGrid(
                columns: [
                    .init(.flexible()),
                    .init(.flexible())
                ]
            ) {
                Section {} header: { Spacer().frame(height: 12) }
                HomeCollectionView(
                    collectionType: .folder,
                    collections: collections,
                    onNavigationCallback: onNavigationCallbackForCollection
                )
                Section {} header: { Spacer().frame(height: 12) }
                HomeCollectionView(
                    collectionType: .tag,
                    collections: collections,
                    onNavigationCallback: { collection in
                        onNavigationCallbackForCollection(collection)
                    }
                )
            }.padding(.horizontal)
        }
    }
}

private struct ToolbarIcons: View {
    let onNavigationCallbackForSearch: () -> Void
    let onNavigationCallbackForSync: () -> Void
    let onNavigationCallbackForSettings: () -> Void
    
    var body: some View {
        #if targetEnvironment(macCatalyst)
        HStack(spacing: 0) {
            MacOSHoverableToolbarIcon(
                bundleKey: "search-01",
                onPressed: onNavigationCallbackForSearch
            )
            /*
            MacOSHoverableToolbarIcon(
                bundleKey: "laptop-phone-sync",
                onPressed: onNavigationCallbackForSync
            )
             */
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
                    onPressed: {}
                )
            }
        }
        #else
        HStack {
            Button {
                onNavigationCallbackForSearch()
            } label: {
                YabaIconView(bundleKey: "search-01")
            }
            #if os(visionOS)
            .buttonStyle(.plain)
            #endif
            Menu {
                ContentAppearancePicker()
                SortingPicker()
                /*
                Divider()
                Button {
                    onNavigationCallbackForSync()
                } label: {
                    Label {
                        Text("Synchronize Label")
                    } icon: {
                        YabaIconView(bundleKey: "laptop-phone-sync")
                    }
                }
                 */
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
            #if os(visionOS)
            .buttonStyle(.plain)
            #endif
        }
        #endif
    }
}

#Preview {
    HomeView(
        onNavigationCallbackForCollection: { _ in },
        onNavigationCallbackForBookmark: { _ in },
        onNavigationCallbackForSearch: {},
        onNavigationCallbackForSettings: {}
    )
    .modelContainer(
        for: [YabaCollection.self, YabaBookmark.self],
        inMemory: true,
    )
}
