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
                    .navigationTitle(collection.label)
                } else {
                    List(selection: $selectedBookmark) {
                        Text("LELE")
                    }
                    .searchable(
                        text: $state.searchQuery,
                        prompt: Text("Search Collection \(collection.label)")
                    )
                    .navigationTitle(collection.label)
                }
            } else {
                ContentUnavailableView(
                    "No Selected Collection Title",
                    systemImage: "square.stack",
                    description: Text("No Selected Collection Message")
                )
            }
        }
    }
}

#Preview {
    CollectionDetail(
        collection: .constant(.empty()),
        selectedBookmark: .constant(.empty())
    )
}
