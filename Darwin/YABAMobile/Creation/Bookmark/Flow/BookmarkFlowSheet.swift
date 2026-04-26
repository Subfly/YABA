//
//  BookmarkFlowSheet.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftUI

struct BookmarkFlowSheet: View {
    let context: BookmarkFlowContext

    @Environment(\.dismiss)
    private var dismiss

    var body: some View {
        Group {
            if let editId = context.editingBookmarkId {
                NavigationStack {
                    BookmarkEditContainer(bookmarkId: editId) {
                        dismiss()
                    }
                }
            } else if let link = context.initialLinkURL?.trimmingCharacters(in: .whitespacesAndNewlines),
                      !link.isEmpty
            {
                NavigationStack {
                    LinkmarkCreationContent(
                        preselectedFolderId: context.preselectedFolderId,
                        preselectedTagIds: context.preselectedTagIds,
                        initialUrl: link,
                        editingBookmarkId: nil,
                        onDone: { dismiss() }
                    )
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
