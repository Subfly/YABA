//
//  FolderItemView.swift
//  YABA
//
//  Full folder row (parity with Compose `FolderItemView`). Nested hierarchy is handled by
//  `OutlineGroup` at list call sites — this view is a single row only. Sheets/alerts stubbed — TODO.
//

import SwiftUI

struct FolderItemView: View {
    @State
    private var itemState = CollectionItemState()

    let folder: FolderModel

    private var isSystemFolder: Bool {
        Constants.Folder.isSystemFolder(folder.folderId)
    }

    var body: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: folder.icon)
                    .scaledToFit()
                    .foregroundStyle(folder.color.getUIColor())
                    .frame(width: 24, height: 24)
                if folder.folderId == Constants.uncategorizedCollectionId {
                    Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                } else {
                    Text(folder.label)
                }
                Spacer(minLength: 0)
                HStack {
                    Text("\(folder.bookmarks.count)")
                        .foregroundStyle(.secondary)
                        .fontWeight(.medium)
                }
                .foregroundStyle(.secondary)
            }
            .buttonStyle(.plain)
        }
        .contentShape(Rectangle())
        .modifier(
            FolderRowInteractionModifier(
                isSystemFolder: isSystemFolder,
                onNewBookmark: { itemState.shouldShowCreateBookmarkSheet = true },
                onEdit: { itemState.shouldShowEditSheet = true },
                onMove: { itemState.shouldShowMoveFolderSheet = true },
                onDelete: { itemState.shouldShowDeleteDialog = true }
            )
        )
        .listRowBackground(
            ItemListRowChrome.listRowBackground(
                cornerRadius: 8,
                isHovered: itemState.isHovered
            )
        )
        .onHover { itemState.isHovered = $0 }
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            FolderCreationContent(existingFolderId: folder.folderId)
        }
        .sheet(isPresented: $itemState.shouldShowMoveFolderSheet) {
            NavigationStack {
                SelectFolderContent(
                    mode: .folderMove,
                    contextFolderId: folder.folderId,
                    onPick: { newParentFolderId in
                        FolderManager.queueMoveFolder(
                            folderId: folder.folderId,
                            newParentFolderId: newParentFolderId
                        )
                    }
                )
            }
        }
        .sheet(isPresented: $itemState.shouldShowCreateBookmarkSheet) {
            // TODO: Create bookmark in folder (pre-select `folder` like Compose `ResultStoreKeys.SELECTED_FOLDER`)
            EmptyView()
        }
        .alert(
            LocalizedStringKey("Delete Folder Title"),
            isPresented: $itemState.shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("Cancel")
            }
            Button(role: .destructive) {
                FolderManager.queueDeleteFolder(folderId: folder.folderId)
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text(LocalizedStringKey("Delete"))
            }
        } message: {
            Text("Delete Content Message \(folder.label)")
        }
    }
}

// MARK: - Context menu & swipe actions (Compose `FolderItemView` parity)

/// System folders: tap/selection only — no overflow, no swipe (`enableContextMenuInteractions` on Compose).
private struct FolderRowInteractionModifier: ViewModifier {
    let isSystemFolder: Bool
    let onNewBookmark: () -> Void
    let onEdit: () -> Void
    let onMove: () -> Void
    let onDelete: () -> Void

    func body(content: Content) -> some View {
        #if KEYBOARD_EXTENSION
        content
        #else
        if isSystemFolder {
            content
        } else {
            content
                .contextMenu {
                    Button {
                        onNewBookmark()
                    } label: {
                        Label {
                            Text(LocalizedStringKey("New Bookmark"))
                        } icon: {
                            YabaIconView(bundleKey: "bookmark-add-02")
                        }
                    }
                    Button {
                        onMove()
                    } label: {
                        Label {
                            Text(LocalizedStringKey("Move"))
                        } icon: {
                            YabaIconView(bundleKey: "arrow-move-up-right")
                        }
                    }
                    Button {
                        onEdit()
                    } label: {
                        Label {
                            Text(LocalizedStringKey("Edit"))
                        } icon: {
                            YabaIconView(bundleKey: "edit-02")
                        }
                    }
                    Divider()
                    Button(role: .destructive) {
                        onDelete()
                    } label: {
                        Label {
                            Text(LocalizedStringKey("Delete"))
                        } icon: {
                            YabaIconView(bundleKey: "delete-02")
                        }
                    }
                }
                .swipeActions(edge: .leading, allowsFullSwipe: false) {
                    Button {
                        onNewBookmark()
                    } label: {
                        swipeLabel(iconKey: "bookmark-add-02", titleKey: "New Bookmark")
                    }
                    .tint(YabaColor.blue.getUIColor())
                    Button {
                        onMove()
                    } label: {
                        swipeLabel(iconKey: "arrow-move-up-right", titleKey: "Move")
                    }
                    .tint(YabaColor.teal.getUIColor())
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button {
                        onEdit()
                    } label: {
                        swipeLabel(iconKey: "edit-02", titleKey: "Edit")
                    }
                    .tint(YabaColor.orange.getUIColor())
                    Button(role: .destructive) {
                        onDelete()
                    } label: {
                        swipeLabel(iconKey: "delete-02", titleKey: "Delete")
                    }
                }
        }
        #endif
    }

    @ViewBuilder
    private func swipeLabel(iconKey: String, titleKey: String) -> some View {
        VStack(spacing: 2) {
            YabaIconView(bundleKey: iconKey)
                .scaledToFit()
                .frame(width: 22, height: 22)
            Text(LocalizedStringKey(titleKey))
                .font(.caption2)
        }
    }
}
