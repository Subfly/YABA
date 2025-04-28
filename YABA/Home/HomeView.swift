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
    
    var body: some View {
        let searching = isSearchActive || !homeState.searchQuery.isEmpty
        
        ZStack {
            #if os(iOS)
            if homeState.shouldShowBackground {
                AnimatedMeshGradient(collectionColor: selectedAppTint)
                    .transition(.identity)
            }
            #endif
            List(selection: $selectedCollection) {
                if searching {
                    HomeSearchView(searchQuery: $homeState.searchQuery)
                } else {
                    HomeCollectionView(
                        collectionType: .tag,
                        isExpanded: $homeState.isTagsExpanded,
                        selectedCollection: $selectedCollection
                    )
                    HomeCollectionView(
                        collectionType: .folder,
                        isExpanded: $homeState.isFoldersExpanded,
                        selectedCollection: $selectedCollection
                    )
                }
            }
            #if os(iOS)
            .scrollContentBackground(.hidden)
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
        #if os(iOS)
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
        .onAppear {
            withAnimation(.smooth.delay(1.25)) {
                homeState.shouldShowBackground = true
            }
        }
        #endif
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
        selectedAppTint: .constant(.accentColor)
    )
    .modelContainer(
        for: [YabaCollection.self, Bookmark.self],
        inMemory: true,
    )
}
