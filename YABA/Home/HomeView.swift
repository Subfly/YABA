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
    
    @State
    private var homeState: HomeState = .init()
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (Bookmark) -> Void
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
        .navigationTitle("YABA")
        .toolbar {
            ToolbarIcons(
                onNavigationCallbackForSearch: onNavigationCallbackForSearch,
                onNavigationCallbackForSettings: onNavigationCallbackForSettings
            )
        }
    }
}

private struct SequentialView: View {
    @Query
    private var collections: [YabaCollection]
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (Bookmark) -> Void
    
    init(
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onNavigationCallbackForCollection: @escaping (YabaCollection) -> Void,
        onNavigationCallbackForBookmark: @escaping (Bookmark) -> Void
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
            HomeCollectionView(
                collectionType: .tag,
                collections: collections.filter { $0.collectionType == .tag },
                onNavigationCallback: onNavigationCallbackForCollection
            )
            HomeCollectionView(
                collectionType: .folder,
                collections: collections.filter { $0.collectionType == .folder },
                onNavigationCallback: onNavigationCallbackForCollection
            )
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
    let onNavigationCallbackForBookmark: (Bookmark) -> Void
    
    init(
        onNavigationCallbackForCollection: @escaping (YabaCollection) -> Void,
        onNavigationCallbackForBookmark: @escaping (Bookmark) -> Void
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
                    collectionType: .tag,
                    collections: collections,
                    onNavigationCallback: { collection in
                        onNavigationCallbackForCollection(collection)
                    }
                )
                Section {} header: { Spacer().frame(height: 12) }
                HomeCollectionView(
                    collectionType: .folder,
                    collections: collections,
                    onNavigationCallback: onNavigationCallbackForCollection
                )
            }.padding(.horizontal)
        }
    }
}

private struct ToolbarIcons: View {
    let onNavigationCallbackForSearch: () -> Void
    let onNavigationCallbackForSettings: () -> Void
    
    var body: some View {
        #if targetEnvironment(macCatalyst)
        HStack(spacing: 0) {
            MacOSHoverableToolbarIcon(
                bundleKey: "search-01",
                onPressed: onNavigationCallbackForSearch
            )
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
                YabaIconView(bundleKey: "more-horizontal-circle-02")
            }
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
        for: [YabaCollection.self, Bookmark.self],
        inMemory: true,
    )
}
