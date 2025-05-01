//
//  HomeSearchView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

struct HomeSearchView: View {
    @Query
    private var bookmarks: [Bookmark]
    
    @Binding
    var selectedBookmark: Bookmark?
    
    @Binding
    var searchQuery: String
    
    let onNavigationCallback: (Bookmark) -> Void
    
    init(
        searchQuery: Binding<String>,
        selectedBookmark: Binding<Bookmark?>,
        onNavigationCallback: @escaping (Bookmark) -> Void
    ) {
        _searchQuery = searchQuery
        _selectedBookmark = selectedBookmark
        self.onNavigationCallback = onNavigationCallback
        
        let query = searchQuery.wrappedValue
        _bookmarks = Query(
            FetchDescriptor(
                predicate: #Predicate { bookmark in
                    if query.isEmpty {
                        true
                    } else {
                        bookmark.label.localizedStandardContains(query)
                        || bookmark.bookmarkDescription.localizedStandardContains(query)
                    }
                }
            )
        )
    }
    
    var body: some View {
        if bookmarks.isEmpty {
            if searchQuery.isEmpty {
                ContentUnavailableView {
                    Label("No Bookmarks Title", systemImage: "bookmark")
                } description: {
                    Text("No Bookmarks Message")
                }
            } else {
                ContentUnavailableView(
                    "Search No Bookmarks Found Title",
                    systemImage: "bookmark.slash",
                    description: Text("Search No Bookmarks Found Description \(searchQuery)")
                )
            }
        } else {
            ForEach(bookmarks) { bookmark in
                BookmarkItemView(
                    selectedBookmark: $selectedBookmark,
                    bookmark: bookmark,
                    isSearching: true,
                    onNavigationCallback: onNavigationCallback
                )
            }
        }
    }
}

#Preview {
    HomeSearchView(
        searchQuery: .constant(""),
        selectedBookmark: .constant(nil),
        onNavigationCallback: { _ in }
    )
}
