//
//  BookmarkFormFolderTagRows.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI

struct BookmarkFormFolderTagRows: View {
    @Query(sort: [SortDescriptor(\FolderModel.label)])
    private var folders: [FolderModel]

    @Query(sort: [SortDescriptor(\TagModel.label)])
    private var allTags: [TagModel]

    let selectedFolderId: String?
    let selectedTagIds: [String]
    let onFolderNavigate: () -> Void
    let onTagsNavigate: () -> Void

    private var folderModel: FolderModel? {
        guard let selectedFolderId else { return nil }
        return folders.first { $0.folderId == selectedFolderId }
    }

    private var selectedTagModels: [TagModel] {
        let set = Set(selectedTagIds)
        return allTags
            .filter { set.contains($0.tagId) }
            .sorted {
                $0.label.localizedStandardCompare($1.label) == .orderedAscending
            }
    }

    private var hasSelectedTags: Bool {
        !selectedTagIds.isEmpty
    }

    var body: some View {
        Section {
            PresentableFolderItemView(
                model: folderModel,
                nullModelPresentableColor: YabaColor.blue
            ) {
                onFolderNavigate()
            }
        } header: {
            Label {
                Text("Folder")
            } icon: {
                YabaIconView(bundleKey: "folder-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }

        Section {
            if !hasSelectedTags {
                ContentUnavailableView {
                    Label {
                        Text("Create Bookmark No Tags Selected Title")
                    } icon: {
                        YabaIconView(bundleKey: "tag-01")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("Create Bookmark No Tags Selected Description")
                }
            } else {
                ForEach(selectedTagModels, id: \.tagId) { tag in
                    HStack {
                        YabaIconView(bundleKey: tag.icon)
                            .frame(width: 24, height: 24)
                            .foregroundStyle(tag.color.getUIColor())
                        Text(tag.label)
                    }
                }
            }
        } header: {
            HStack {
                Label {
                    Text("Tags")
                } icon: {
                    YabaIconView(bundleKey: "tag-01")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                Spacer()
                Button {
                    onTagsNavigate()
                } label: {
                    Label {
                        Text(
                            LocalizedStringKey(
                                hasSelectedTags
                                    ? "Create Bookmark Edit Tags"
                                    : "Create Bookmark Add Tags"
                            )
                        )
                        .textCase(.none)
                    } icon: {
                        YabaIconView(
                            bundleKey: hasSelectedTags
                                ? "edit-02"
                                : "plus-sign"
                        )
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                    }
                }
                .buttonStyle(.borderless)
            }
        }
    }
}
