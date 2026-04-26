//
//  BookmarkCreationRouting.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import Foundation
import SwiftUI

struct BookmarkTypeSelectionContext: Identifiable, Equatable {
    let id: UUID
    let preselectedFolderId: String?
    let preselectedTagIds: [String]

    init(
        id: UUID = UUID(),
        preselectedFolderId: String? = nil,
        preselectedTagIds: [String] = []
    ) {
        self.id = id
        self.preselectedFolderId = preselectedFolderId
        self.preselectedTagIds = preselectedTagIds
    }
}

struct BookmarkKindFormLaunch: Identifiable, Equatable {
    let id: UUID
    let kind: BookmarkKind
    let preselectedFolderId: String?
    let preselectedTagIds: [String]

    init(
        id: UUID = UUID(),
        kind: BookmarkKind,
        preselectedFolderId: String?,
        preselectedTagIds: [String]
    ) {
        self.id = id
        self.kind = kind
        self.preselectedFolderId = preselectedFolderId
        self.preselectedTagIds = preselectedTagIds
    }
}

struct BookmarkCreateTwoStepSheetsModifier: ViewModifier {
    @Binding
    var typeSelection: BookmarkTypeSelectionContext?

    @State
    private var kindLaunch: BookmarkKindFormLaunch?

    @State
    private var pendingKindAfterTypeDismiss: BookmarkKindFormLaunch?

    func body(content: Content) -> some View {
        content
            .sheet(
                item: $typeSelection,
                onDismiss: {
                    if let next = pendingKindAfterTypeDismiss {
                        kindLaunch = next
                        pendingKindAfterTypeDismiss = nil
                    }
                }
            ) { ctx in
                BookmarkRouteSelectionContent(
                    onCancel: {
                        pendingKindAfterTypeDismiss = nil
                        typeSelection = nil
                    },
                    onSelectKind: { kind in
                        pendingKindAfterTypeDismiss = BookmarkKindFormLaunch(
                            kind: kind,
                            preselectedFolderId: ctx.preselectedFolderId,
                            preselectedTagIds: ctx.preselectedTagIds
                        )
                        typeSelection = nil
                    }
                )
                .presentationDetents([.fraction(0.44)])
                .presentationDragIndicator(.visible)
            }
            .sheet(item: $kindLaunch) { launch in
                BookmarkKindCreationSheet(launch: launch) {
                    kindLaunch = nil
                }
            }
    }
}

extension View {
    func bookmarkCreateTwoStepSheets(typeSelection: Binding<BookmarkTypeSelectionContext?>) -> some View {
        modifier(BookmarkCreateTwoStepSheetsModifier(typeSelection: typeSelection))
    }
}
