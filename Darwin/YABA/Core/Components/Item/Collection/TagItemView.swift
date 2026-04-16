//
//  TagItemView.swift
//  YABA
//
//  Full tag row (Compose parity). Sheets/alerts stubbed — TODO.
//

import SwiftUI

struct TagItemView: View {
    @State
    private var itemState = CollectionItemState()

    let tag: TagModel

    private var isSystemTag: Bool {
        Constants.Tag.isSystemTag(tag.tagId)
    }

    var body: some View {
        HStack {
            HStack {
                YabaIconView(bundleKey: tag.icon)
                    .scaledToFit()
                    .foregroundStyle(tag.color.getUIColor())
                    .frame(width: 24, height: 24)
                Text(tag.label)
            }
            Spacer()
            HStack {
                Text("\(tag.bookmarks.count)")
                    .foregroundStyle(.secondary)
                    .fontWeight(.medium)
            }
            .foregroundStyle(.secondary)
        }
        .buttonStyle(.plain)
        .contentShape(Rectangle())
        .modifier(
            TagRowInteractionModifier(
                isSystemTag: isSystemTag,
                onNewBookmark: { itemState.shouldShowCreateBookmarkSheet = true },
                onEdit: { itemState.shouldShowEditSheet = true },
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
            TagCreationContent(existingTagId: tag.tagId)
        }
        .sheet(isPresented: $itemState.shouldShowCreateBookmarkSheet) {
            // TODO: Create bookmark with tag (pre-select `tag` like Compose `ResultStoreKeys.SELECTED_TAGS`)
            EmptyView()
        }
        .alert(
            LocalizedStringKey("Delete Tag Title"),
            isPresented: $itemState.shouldShowDeleteDialog
        ) {
            Button(role: .cancel) {
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text("Cancel")
            }
            Button(role: .destructive) {
                TagManager.queueDeleteOrHideTag(tagId: tag.tagId)
                itemState.shouldShowDeleteDialog = false
            } label: {
                Text(LocalizedStringKey("Delete"))
            }
        } message: {
            Text("Delete Content Message \(tag.label)")
        }
    }
}

// MARK: - Context menu & swipe actions (Compose `TagItemView` parity)

/// System tags: tap/selection only — no overflow, no swipe.
private struct TagRowInteractionModifier: ViewModifier {
    let isSystemTag: Bool
    let onNewBookmark: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    func body(content: Content) -> some View {
        #if KEYBOARD_EXTENSION
        content
        #else
        if isSystemTag {
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
