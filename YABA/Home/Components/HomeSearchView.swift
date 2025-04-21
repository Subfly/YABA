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
    
    init(searchQuery: String) {
        _bookmarks = Query(
            FetchDescriptor(
                predicate: #Predicate { bookmark in
                    if searchQuery.isEmpty {
                        true
                    } else {
                        bookmark.label.localizedStandardContains(searchQuery) || bookmark.bookmarkDescription.localizedStandardContains(searchQuery)
                    }
                }
            )
        )
    }
    
    var body: some View {
        if bookmarks.isEmpty {
            ContentUnavailableView {
                Label("No Bookmarks Title", systemImage: "bookmark")
            } description: {
                Text("No Bookmarks Message")
            }
        } else {
            ForEach(bookmarks) { bookmark in
                Text(bookmark.label)
            }
        }
    }
}

#Preview {
    HomeSearchView(searchQuery: "")
}
