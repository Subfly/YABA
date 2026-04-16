//
//  BookmarkKindCreationSheet.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftUI

struct BookmarkKindCreationSheet: View {
    let launch: BookmarkKindFormLaunch
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            BookmarkKindForm(
                mode: .create(
                    launch.kind,
                    preselectedFolderId: launch.preselectedFolderId,
                    preselectedTagIds: launch.preselectedTagIds
                ),
                onDone: onDone
            )
        }
        .presentationDetents([.large])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
    }
}
