//
//  HomeRecentsView.swift
//  YABA
//
//  Created by Ali Taha on 5.06.2025.
//

import SwiftUI
import SwiftData

struct HomeRecentsView: View {
    @Query(sort: \YabaBookmark.editedAt, order: .reverse, animation: .smooth)
    private var bookmarks: [YabaBookmark]
    
    let onNavigationCallback: (YabaBookmark) -> Void
    
    var body: some View {
        if !bookmarks.isEmpty {
            Section {
                ForEach(bookmarks.prefix(5)) { bookmark in
                    BookmarkItemView(
                        bookmark: bookmark,
                        onNavigationCallback: onNavigationCallback
                    )
                }
            } header: {
                Label {
                    Text("Home Recents Label")
                } icon: {
                    YabaIconView(bundleKey: "clock-01")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
    }
}

#Preview {
    HomeRecentsView(onNavigationCallback: { _ in })
}
