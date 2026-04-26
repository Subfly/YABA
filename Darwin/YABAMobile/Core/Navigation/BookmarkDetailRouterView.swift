//
//  BookmarkDetailRouterView.swift
//  YABA
//
//  Routes bookmark detail destinations by `BookmarkKind` (link vs image, etc.).
//

import SwiftData
import SwiftUI

struct BookmarkDetailRouterView: View {
    let bookmarkId: String
    let onOpenFolder: (String) -> Void
    let onOpenTag: (String) -> Void

    @Query
    private var bookmarks: [YabaBookmark]

    init(
        bookmarkId: String,
        onOpenFolder: @escaping (String) -> Void = { _ in },
        onOpenTag: @escaping (String) -> Void = { _ in }
    ) {
        self.bookmarkId = bookmarkId
        self.onOpenFolder = onOpenFolder
        self.onOpenTag = onOpenTag
        var d = FetchDescriptor<YabaBookmark>(
            predicate: #Predicate<YabaBookmark> { $0.bookmarkId == bookmarkId }
        )
        d.fetchLimit = 1
        _bookmarks = Query(d, animation: .smooth)
    }

    var body: some View {
        Group {
            if let bm = bookmarks.first {
                switch bm.kind {
                case .link:
                    LinkmarkDetailView(
                        bookmarkId: bookmarkId,
                        onOpenFolder: onOpenFolder,
                        onOpenTag: onOpenTag
                    )
                case .image:
                    ImagemarkDetailView(
                        bookmarkId: bookmarkId,
                        onOpenFolder: onOpenFolder,
                        onOpenTag: onOpenTag
                    )
                default:
                    EmptyView()
                }
            } else {
                EmptyView()
            }
        }
    }
}
