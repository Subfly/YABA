//
//  BookmarkCreationFolderVisuals.swift
//  YABA
//
//  Single-folder SwiftData read + virtual Uncategorized for bookmark creation forms
//  (avoids `@Query` over all folders).
//

import SwiftData
import SwiftUI

/// Supplies the resolved folder model for UI (including virtual Uncategorized) and a tint color.
struct BookmarkCreationFolderVisuals<Content: View>: View {
    let folderId: String?
    let uncategorizedCreationRequired: Bool
    @ViewBuilder let content: (FolderModel?, Color) -> Content

    @Query
    private var dbFolders: [FolderModel]

    init(
        folderId: String?,
        uncategorizedCreationRequired: Bool,
        @ViewBuilder content: @escaping (FolderModel?, Color) -> Content
    ) {
        self.folderId = folderId
        self.uncategorizedCreationRequired = uncategorizedCreationRequired
        self.content = content
        if let fid = folderId,
           !(uncategorizedCreationRequired && fid == Constants.uncategorizedCollectionId)
        {
            let cap = fid
            _dbFolders = Query(
                filter: #Predicate<FolderModel> { $0.folderId == cap },
                sort: [],
                animation: .default
            )
        } else {
            _dbFolders = Query(
                filter: #Predicate<FolderModel> { _ in false },
                sort: [],
                animation: .default
            )
        }
    }

    private var resolvedFolder: FolderModel? {
        guard let folderId else { return nil }
        if uncategorizedCreationRequired, folderId == Constants.uncategorizedCollectionId {
            return FolderManager.virtualUncategorizedFolderModel()
        }
        return dbFolders.first
    }

    private var mainTint: Color {
        if let folderId, uncategorizedCreationRequired, folderId == Constants.uncategorizedCollectionId {
            return YabaColor.blue.getUIColor()
        }
        return dbFolders.first?.color.getUIColor() ?? .accentColor
    }

    var body: some View {
        content(resolvedFolder, mainTint)
    }
}
