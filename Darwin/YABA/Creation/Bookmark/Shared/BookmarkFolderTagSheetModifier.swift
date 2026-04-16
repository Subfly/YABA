//
//  BookmarkFolderTagSheetModifier.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftUI

struct BookmarkFolderTagSheetModifier: ViewModifier {
    @Binding
    var showFolderSheet: Bool

    @Binding
    var showTagSheet: Bool

    let tagSelectionInitialIds: [String]
    let onFolderPicked: (String?) -> Void
    let onTagsPicked: ([String]) -> Void

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: $showFolderSheet) {
                NavigationStack {
                    SelectFolderContent(mode: .folderSelection, onPick: onFolderPicked)
                }
            }
            .sheet(isPresented: $showTagSheet) {
                NavigationStack {
                    BookmarkTagSelectionContent(
                        initialTagIds: tagSelectionInitialIds,
                        onDone: onTagsPicked
                    )
                }
            }
    }
}

extension View {
    func bookmarkFolderAndTagSheets(
        showFolderSheet: Binding<Bool>,
        showTagSheet: Binding<Bool>,
        tagSelectionInitialIds: [String],
        onFolderPicked: @escaping (String?) -> Void,
        onTagsPicked: @escaping ([String]) -> Void
    ) -> some View {
        modifier(
            BookmarkFolderTagSheetModifier(
                showFolderSheet: showFolderSheet,
                showTagSheet: showTagSheet,
                tagSelectionInitialIds: tagSelectionInitialIds,
                onFolderPicked: onFolderPicked,
                onTagsPicked: onTagsPicked
            )
        )
    }
}
