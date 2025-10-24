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
            HStack {
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
                .font(.headline)
                Spacer()
            }
            .contentShape(Rectangle())
            .padding(.horizontal)
            .padding(.bottom)
            
            ForEach(bookmarks.prefix(5)) { bookmark in
                BookmarkItemView(
                    bookmark: bookmark,
                    isInRecents: true,
                    isSelected: false,
                    isInSelectionMode: false,
                    onNavigationCallback: onNavigationCallback
                )
                Spacer().frame(height: 6)
            }
        }
    }
}

#Preview {
    HomeRecentsView(onNavigationCallback: { _ in })
}
