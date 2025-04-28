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
    var searchQuery: String
    
    init(searchQuery: Binding<String>) {
        _searchQuery = searchQuery
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
                Text(bookmark.label)
            }
        }
    }
}

#Preview {
    HomeSearchView(searchQuery: .constant(""))
}
