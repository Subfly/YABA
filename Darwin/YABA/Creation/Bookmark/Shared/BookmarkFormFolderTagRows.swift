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
        return allTags.filter { set.contains($0.tagId) }
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
            Button {
                onTagsNavigate()
            } label: {
                HStack {
                    if selectedTagModels.isEmpty {
                        Text(LocalizedStringKey("Select Tags No Tags Selected Message"))
                            .foregroundStyle(.secondary)
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(selectedTagModels, id: \.tagId) { tag in
                                HStack {
                                    YabaIconView(bundleKey: tag.icon)
                                        .scaledToFit()
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 22, height: 22)
                                    Text(tag.label)
                                    Spacer()
                                }
                            }
                        }
                    }
                    Spacer(minLength: 0)
                    YabaIconView(bundleKey: "arrow-right-01")
                        .frame(width: 22, height: 22)
                }
            }
            .buttonStyle(.plain)
        } header: {
            Label {
                Text("Tags")
            } icon: {
                YabaIconView(bundleKey: "tag-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}
