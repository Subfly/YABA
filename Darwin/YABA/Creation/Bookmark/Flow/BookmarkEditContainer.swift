//
//  BookmarkEditContainer.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI

struct BookmarkEditContainer: View {
    let bookmarkId: String
    let onDone: () -> Void

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var bookmark: BookmarkModel?

    var body: some View {
        Group {
            if let bookmark {
                BookmarkKindForm(mode: .edit(bookmark), onDone: onDone)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task(id: bookmarkId) {
            bookmark = BookmarkFlowHydration.fetchBookmark(
                bookmarkId: bookmarkId,
                modelContext: modelContext
            )
        }
    }
}
