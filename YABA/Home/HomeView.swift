//
//  HomeView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

struct HomeView: View {
    @State
    private var homeState: HomeState = .init()
    
    @FocusState
    private var isSearchActive: Bool
    
    @Binding
    var selectedCollection: YabaCollection?
    
    @Binding
    var selectedAppTint: Color
    
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        let searching = isSearchActive || !homeState.searchQuery.isEmpty
        
        ZStack {
            #if os(iOS)
            AnimatedMeshGradient(collectionColor: selectedAppTint)
            
            #endif
            List(selection: $selectedCollection) {
                if searching {
                    HomeSearchView(searchQuery: $homeState.searchQuery)
                } else {
                    HomeCollectionView(
                        collectionType: .tag,
                        isExpanded: $homeState.isTagsExpanded,
                        selectedCollection: $selectedCollection,
                        onNavigationCallback: onNavigationCallback
                    )
                    HomeCollectionView(
                        collectionType: .folder,
                        isExpanded: $homeState.isFoldersExpanded,
                        selectedCollection: $selectedCollection,
                        onNavigationCallback: onNavigationCallback
                    )
                }
            }
            #if os(iOS)
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
    }
}

#Preview {
    HomeView(
        selectedCollection: .constant(.empty()),
        selectedAppTint: .constant(.accentColor),
        onNavigationCallback: { _ in }
    )
    .modelContainer(
        for: [YabaCollection.self, Bookmark.self],
        inMemory: true,
    )
}
