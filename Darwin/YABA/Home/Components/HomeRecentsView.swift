//
//  HomeRecentsView.swift
//  YABA
//
//  Created by Ali Taha on 5.06.2025.
//

import SwiftData
import SwiftUI

struct HomeRecentsView: View {
    @Query
    private var bookmarks: [YabaBookmark]

    init() {
        var descriptor = FetchDescriptor<YabaBookmark>(
            sortBy: [SortDescriptor(\.editedAt, order: .reverse)]
        )
        descriptor.fetchLimit = 5
        _bookmarks = Query(descriptor, animation: .smooth)
    }

    var body: some View {
        if !bookmarks.isEmpty {
            Section {
                ForEach(bookmarks) { bookmark in
                    BookmarkItemView(
                        bookmark: bookmark,
                        isInRecents: true,
                        isSelected: false,
                        isInSelectionMode: false,
                        onNavigationCallback: { _ in }
                    )
                }
            } header: {
                Label {
                    Text("Home Recents Label")
                        .font(.headline)
                        .fontWeight(.semibold)
                } icon: {
                    YabaIconView(bundleKey: "clock-01")
                        .scaledToFit()
                        .frame(width: 22, height: 22)
                }
                .foregroundStyle(.secondary)
            }
        }
    }
}
