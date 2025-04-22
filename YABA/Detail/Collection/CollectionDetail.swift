//
//  CollectionDetail.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

struct CollectionDetail: View {
    @State
    private var state: CollectionDetailState = .init()
    
    @Binding
    var collection: YabaCollection?
    
    @Binding
    var selectedBookmark: Bookmark?
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(
                collectionColor: collection?.color.getUIColor() ?? .accentColor
            )
            if let collection {
                if collection.bookmarks.isEmpty {
                    ContentUnavailableView(
                        "No Bookmarks Title",
                        systemImage: "bookmark",
                        description: Text("No Bookmarks Message")
                    )
                } else {
                    List(selection: $selectedBookmark) {
                        ForEach(
                            collection.bookmarks.filter {
                                if state.searchQuery.isEmpty {
                                    true
                                } else {
                                    $0.label.localizedStandardContains(state.searchQuery)
                                    || $0.bookmarkDescription.localizedStandardContains(state.searchQuery)
                                }
                            }
                        ) { bookmark in
                            BookmarkItemView(
                                selectedBookmark: $selectedBookmark,
                                bookmark: bookmark
                            )
                        }
                    }
                    .listStyle(.sidebar)
                    .scrollContentBackground(.hidden)
                    .searchable(
                        text: $state.searchQuery,
                        prompt: Text("Search Collection \(collection.label)")
                    )
                }
            } else {
                ContentUnavailableView(
                    "No Selected Collection Title",
                    systemImage: "square.stack",
                    description: Text("No Selected Collection Message")
                )
            }
        }
        .navigationTitle(collection?.label ?? "")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    state.shouldShowCreateBookmarkSheet = true
                } label: {
                    Image(systemName: "plus.circle")
                }
            }
        }
        .sheet(isPresented: $state.shouldShowCreateBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: .constant(nil),
                initialCollection: $collection
            )
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .constant(.empty()),
        selectedBookmark: .constant(.empty())
    )
}
