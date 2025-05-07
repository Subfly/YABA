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
    
    @FocusState
    private var isSearchActive: Bool
    
    @Binding
    var selectedCollection: YabaCollection?
    
    @Binding
    var selectedBookmark: Bookmark?
    
    @Binding
    var selectedAppTint: Color
    
    let onNavigationCallbackForCollection: (YabaCollection) -> Void
    let onNavigationCallbackForBookmark: (Bookmark) -> Void
    let onNavigationCallbackForSettings: () -> Void
    
    var body: some View {
        let searching = isSearchActive || !homeState.searchQuery.isEmpty
        
        ZStack {
            #if !targetEnvironment(macCatalyst)
            AnimatedMeshGradient(collectionColor: selectedAppTint)
            #endif
            // TODO: CHANGE THIS WHEN LAZYVGRID IS RECYCABLE
            sequentailView
                .scrollContentBackground(.hidden)
                .searchable(
                    text: $homeState.searchQuery,
                    prompt: "Home Search Prompt"
                )
                .searchFocused($isSearchActive)
                .onChange(of: searching) { _, isSearching in
                    if isSearching {
                        withAnimation {
                            homeState.isFABActive = false
                        }
                    }
                }
                #if targetEnvironment(macCatalyst)
                .listRowSpacing(2)
                .listStyle(.sidebar)
                #endif
            
            HomeCreateContentFAB(
                isActive: $homeState.isFABActive,
                selectedAppTint: $selectedAppTint,
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
            .opacity(!searching ? 1 : 0)
            .transition(.blurReplace)
            .ignoresSafeArea()
        }
        .sheet(isPresented: $homeState.shouldShowCreateContentSheet) {
            if let creationType = homeState.selectedContentCreationType {
                CollectionCreationContent(
                    collectionType: creationType,
                    collectionToEdit: .constant(nil),
                    onEditCallback: { _ in }
                )
            }
        }
        .sheet(isPresented: $homeState.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: .constant(nil),
                initialCollection: $selectedCollection,
                link: nil,
                onExitRequested: {}
            )
        }
        .navigationTitle("YABA")
        .toolbar {
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
    }
    
    @ViewBuilder
    private var sequentailView: some View {
        let searching = isSearchActive || !homeState.searchQuery.isEmpty
        
        List(selection: $selectedCollection) {
            if searching {
                HomeSearchView(
                    searchQuery: $homeState.searchQuery,
                    selectedBookmark: $selectedBookmark,
                    onNavigationCallback: onNavigationCallbackForBookmark
                )
            } else {
                HomeCollectionView(
                    collectionType: .tag,
                    isExpanded: $homeState.isTagsExpanded,
                    selectedCollection: $selectedCollection,
                    selectedSorting: preferredSorting,
                    selectedSortOrder: preferredSortOrder,
                    onNavigationCallback: onNavigationCallbackForCollection
                )
                HomeCollectionView(
                    collectionType: .folder,
                    isExpanded: $homeState.isFoldersExpanded,
                    selectedCollection: $selectedCollection,
                    selectedSorting: preferredSorting,
                    selectedSortOrder: preferredSortOrder,
                    onNavigationCallback: onNavigationCallbackForCollection
                )
            }
        }
    }
    
    @ViewBuilder
    private var gridView: some View {
        let searching = isSearchActive || !homeState.searchQuery.isEmpty
        
        ScrollView {
            LazyVGrid(
                columns: [
                    .init(.flexible()),
                    .init(.flexible())
                ]
            ) {
                if searching {
                    HomeSearchView(
                        searchQuery: $homeState.searchQuery,
                        selectedBookmark: $selectedBookmark,
                        onNavigationCallback: onNavigationCallbackForBookmark
                    )
                } else {
                    Section {} header: { Spacer().frame(height: 12) }
                    HomeCollectionView(
                        collectionType: .tag,
                        isExpanded: $homeState.isTagsExpanded,
                        selectedCollection: $selectedCollection,
                        selectedSorting: preferredSorting,
                        selectedSortOrder: preferredSortOrder,
                        onNavigationCallback: onNavigationCallbackForCollection
                    )
                    Section {} header: { Spacer().frame(height: 12) }
                    HomeCollectionView(
                        collectionType: .folder,
                        isExpanded: $homeState.isFoldersExpanded,
                        selectedCollection: $selectedCollection,
                        selectedSorting: preferredSorting,
                        selectedSortOrder: preferredSortOrder,
                        onNavigationCallback: onNavigationCallbackForCollection
                    )
                }
            }.padding(.horizontal)
        }
    }
}

#Preview {
    HomeView(
        selectedCollection: .constant(.empty()),
        selectedBookmark: .constant(.empty()),
        selectedAppTint: .constant(.accentColor),
        onNavigationCallbackForCollection: { _ in },
        onNavigationCallbackForBookmark: { _ in },
        onNavigationCallbackForSettings: {}
    )
    .modelContainer(
        for: [YabaCollection.self, Bookmark.self],
        inMemory: true,
    )
}
