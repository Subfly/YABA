//
//  CollectionDetail.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

struct CollectionDetail: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    @State
    private var state: CollectionDetailState = .init()
    
    @Binding
    var collection: YabaCollection?
    
    @Binding
    var selectedBookmark: Bookmark?
    
    let onNavigationCallback: (Bookmark) -> Void
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(
                collectionColor: collection?.color.getUIColor() ?? .accentColor
            )
            if let collection {
                if collection.bookmarks.isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("No Bookmarks Title")
                        } icon: {
                            YabaIconView(bundleKey: "bookmark-02")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text("No Bookmarks Message")
                    }
                } else {
                    searchableContent
                        .searchable(
                            text: $state.searchQuery,
                            prompt: Text("Search Collection \(collection.label)")
                        )
                }
            } else {
                ContentUnavailableView {
                    Label {
                        Text("No Selected Collection Title")
                    } icon: {
                        YabaIconView(bundleKey: "dashboard-square-02")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("No Selected Collection Message")
                }
            }
        }
        .navigationTitle(collection?.label ?? "")
        .toolbar { toolbarItem }
        .sheet(isPresented: $state.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: .constant(nil),
                initialCollection: $collection,
                link: nil,
                onExitRequested: {}
            )
        }
    }
    
    @ViewBuilder
    private var searchableContent: some View {
        // Thanks SwiftData...
        var filtered: [Bookmark] {
            let queried: [Bookmark] = collection?.bookmarks.filter { bookmark in
                if state.searchQuery.isEmpty {
                    true
                } else {
                    bookmark.label.localizedStandardContains(state.searchQuery)
                    || bookmark.bookmarkDescription.localizedStandardContains(state.searchQuery)
                }
            } ?? []
            
            let sorted = queried.sorted {
                switch preferredSorting {
                case .createdAt:
                    switch preferredSortOrder {
                    case .ascending:
                        $0.createdAt < $1.createdAt
                    case .descending:
                        $0.createdAt > $1.createdAt
                    }
                case .editedAt:
                    switch preferredSortOrder {
                    case .ascending:
                        $0.editedAt < $1.editedAt
                    case .descending:
                        $0.editedAt > $1.editedAt
                    }
                case .label:
                    switch preferredSortOrder {
                    case .ascending:
                        $0.label < $1.label
                    case .descending:
                        $0.label > $1.label
                    }
                }
            }
            
            return sorted
        }
        
        if filtered.isEmpty {
            ContentUnavailableView {
                Label {
                    Text("Search No Bookmarks Found Title")
                } icon: {
                    YabaIconView(bundleKey: "bookmark-off-02")
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                }
            } description: {
                Text("Search No Bookmarks Found Description \(state.searchQuery)")
            }
        } else {
            switch contentAppearance {
            case .list:
                List(selection: $selectedBookmark) {
                    ForEach(filtered) { bookmark in
                        BookmarkItemView(
                            selectedBookmark: $selectedBookmark,
                            bookmark: bookmark,
                            isSearching: false,
                            onNavigationCallback: onNavigationCallback
                        )
                    }
                }
                .listStyle(.sidebar)
                .scrollContentBackground(.hidden)
            case .grid:
                ScrollView {
                    LazyVGrid(
                        columns: [
                            .init(.flexible()),
                            .init(.flexible())
                        ]
                    ) {
                        ForEach(filtered) { bookmark in
                            BookmarkItemView(
                                selectedBookmark: $selectedBookmark,
                                bookmark: bookmark,
                                isSearching: false,
                                onNavigationCallback: onNavigationCallback
                            )
                        }
                    }.padding()
                }
                .scrollContentBackground(.hidden)
            }
        }
    }
    
    @ViewBuilder
    private var toolbarItem: some View {
        if collection != nil {
            #if !targetEnvironment(macCatalyst)
            if UIDevice.current.userInterfaceIdiom == .phone {
                Menu {
                    Button {
                        state.shouldShowCreateBookmarkSheet = true
                    } label: {
                        Label {
                            Text("New")
                        } icon: {
                            YabaIconView(bundleKey: "plus-sign-circle")
                        }
                    }
                    ContentAppearancePicker()
                    SortingPicker()
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                }
            } else {
                Button {
                    state.shouldShowCreateBookmarkSheet = true
                } label: {
                    YabaIconView(bundleKey: "plus-sign-circle")
                }
            }
            #else
            MacOSHoverableToolbarIcon(
                bundleKey: "plus-sign-circle",
                onPressed: {
                    state.shouldShowCreateBookmarkSheet = true
                }
            )
            #endif
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .constant(.empty()),
        selectedBookmark: .constant(.empty()),
        onNavigationCallback: { _ in }
    )
}
