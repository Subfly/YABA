//
//  HomeSearchView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import SwiftData

// TODO: MOVE IT TO DETAIL, NOT LET BE A PART OF HOME
struct HomeSearchView: View {
    @Query
    private var bookmarks: [Bookmark]
    
    @Binding
    var searchQuery: String
    let onNavigationCallback: (Bookmark) -> Void
    
    init(
        searchQuery: Binding<String>,
        onNavigationCallback: @escaping (Bookmark) -> Void
    ) {
        _searchQuery = searchQuery
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
                ContentUnavailableView {
                    Label {
                        Text("Search No Bookmarks Found Title")
                    } icon: {
                        YabaIconView(bundleKey: "bookmark-off-02")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("Search No Bookmarks Found Description \(searchQuery)")
                }
            }
        } else {
            ForEach(bookmarks) { bookmark in
                BookmarkItemView(
                    bookmark: bookmark,
                    onNavigationCallback: onNavigationCallback
                )
            }
        }
    }
}

#Preview {
    HomeSearchView(
        searchQuery: .constant(""),
        onNavigationCallback: { _ in }
    )
}
