//
//  BookmarkKindForm.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftUI

struct BookmarkKindForm: View {
    enum Mode {
        case create(BookmarkKind, preselectedFolderId: String?, preselectedTagIds: [String])
        case edit(BookmarkModel)
    }

    let mode: Mode
    let onDone: () -> Void

    var body: some View {
        switch mode {
        case let .create(kind, folderId, tagIds):
            switch kind {
            case .link:
                LinkmarkCreationContent(
                    preselectedFolderId: folderId,
                    preselectedTagIds: tagIds,
                    initialUrl: nil,
                    editingBookmarkId: nil,
                    onDone: onDone
                )
            case .note:
                NotemarkCreationContent(
                    preselectedFolderId: folderId,
                    preselectedTagIds: tagIds,
                    editingBookmarkId: nil,
                    onDone: onDone
                )
            case .image:
                ImagemarkCreationContent(
                    preselectedFolderId: folderId,
                    preselectedTagIds: tagIds,
                    editingBookmarkId: nil,
                    onDone: onDone
                )
            case .file:
                DocmarkCreationContent(
                    preselectedFolderId: folderId,
                    preselectedTagIds: tagIds,
                    editingBookmarkId: nil,
                    onDone: onDone
                )
            case .canvas:
                CanvmarkCreationContent(
                    preselectedFolderId: folderId,
                    preselectedTagIds: tagIds,
                    editingBookmarkId: nil,
                    onDone: onDone
                )
            }
        case let .edit(bookmark):
            switch bookmark.kind {
            case .link:
                LinkmarkCreationContent(
                    preselectedFolderId: nil,
                    preselectedTagIds: [],
                    initialUrl: nil,
                    editingBookmarkId: bookmark.bookmarkId,
                    onDone: onDone
                )
            case .note:
                NotemarkCreationContent(
                    preselectedFolderId: nil,
                    preselectedTagIds: [],
                    editingBookmarkId: bookmark.bookmarkId,
                    onDone: onDone
                )
            case .image:
                ImagemarkCreationContent(
                    preselectedFolderId: nil,
                    preselectedTagIds: [],
                    editingBookmarkId: bookmark.bookmarkId,
                    onDone: onDone
                )
            case .file:
                DocmarkCreationContent(
                    preselectedFolderId: nil,
                    preselectedTagIds: [],
                    editingBookmarkId: bookmark.bookmarkId,
                    onDone: onDone
                )
            case .canvas:
                CanvmarkCreationContent(
                    preselectedFolderId: nil,
                    preselectedTagIds: [],
                    editingBookmarkId: bookmark.bookmarkId,
                    onDone: onDone
                )
            }
        }
    }
}
