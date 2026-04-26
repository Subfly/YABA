//
//  ImagemarkDetailInfoSheet.swift
//  YABA
//
//  Single info section (no segmented tabs): mirrors Compose imagemark detail layout.
//

import SwiftData
import SwiftUI

struct ImagemarkDetailInfoSheet: View {
    @Environment(\.dismiss)
    private var dismiss

    let bookmark: YabaBookmark
    let folderAccent: Color
    let reminderDate: Date?
    let onDeleteReminder: () -> Void
    let onOpenFolder: (String) -> Void
    let onOpenTag: (String) -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    infoTextRow(
                        icon: "text",
                        value: bookmark.label
                    )
                    infoTextRow(
                        icon: "paragraph",
                        value: bookmark.bookmarkDescription?.trimmingCharacters(in: .whitespacesAndNewlines),
                        emptyPlaceholder: "Bookmark Detail No Description Provided"
                    )
                    infoMetadataRow(
                        icon: "clock-01",
                        title: "Bookmark Detail Created At Title",
                        value: bookmark.createdAt.formatted(date: .abbreviated, time: .shortened)
                    )
                    if bookmark.createdAt != bookmark.editedAt {
                        infoMetadataRow(
                            icon: "edit-02",
                            title: "Bookmark Detail Edited At Title",
                            value: bookmark.editedAt.formatted(date: .abbreviated, time: .shortened)
                        )
                    }
                    if let reminderDate {
                        infoMetadataRow(
                            icon: "notification-01",
                            title: "Bookmark Detail Remind Me Title",
                            value: reminderDate.formatted(date: .abbreviated, time: .shortened)
                        )
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                onDeleteReminder()
                            } label: {
                                VStack(spacing: 2) {
                                    YabaIconView(bundleKey: "delete-02")
                                        .frame(width: 22, height: 22)
                                    Text("Delete")
                                        .font(.caption2)
                                }
                            }
                            .tint(.red)
                        }
                    }
                } header: {
                    sectionHeader("Info", icon: "information-circle")
                }

                if let folder = bookmark.folder {
                    Section {
                        PresentableFolderItemView(
                            model: folder,
                            nullModelPresentableColor: .blue,
                            onPressed: {
                                onOpenFolder(folder.folderId)
                            }
                        )
                    } header: {
                        sectionHeader("Folder", icon: "folder-01")
                    }
                }

                Section {
                    if bookmark.tags.isEmpty {
                        ContentUnavailableView {
                            Label {
                                Text("Bookmark Detail No Tags Added Title")
                            } icon: {
                                YabaIconView(bundleKey: "tags")
                                    .scaledToFit()
                                    .frame(width: 52, height: 52)
                                    .foregroundStyle(folderAccent)
                            }
                        } description: {
                            Text("Bookmark Detail No Tags Added Description")
                        }
                    } else {
                        ForEach(bookmark.tags) { tag in
                            PresentableTagItemView(
                                model: tag,
                                nullModelPresentableColor: .blue,
                                onPressed: {
                                    onOpenTag(tag.tagId)
                                },
                                onNavigateToEdit: {}
                            )
                        }
                    }
                } header: {
                    sectionHeader("Tags Title", icon: "tag-01")
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
            .tint(folderAccent)
            .navigationTitle("Bookmark Detail Sheet Navigation Title")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }

    private func infoTextRow(
        icon: String,
        value: String?,
        emptyPlaceholder: LocalizedStringKey? = nil
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            YabaIconView(bundleKey: icon)
                .frame(width: 22, height: 22)
                .foregroundStyle(folderAccent)
                .padding(.top, 1)

            if let value = value?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty {
                Text(value)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
            } else if let emptyPlaceholder {
                Text(emptyPlaceholder)
                    .foregroundStyle(.secondary)
                    .italic()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .multilineTextAlignment(.leading)
            }
        }
    }

    private func infoMetadataRow(icon: String, title: LocalizedStringKey, value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            HStack(alignment: .center, spacing: 10) {
                YabaIconView(bundleKey: icon)
                    .frame(width: 22, height: 22)
                    .foregroundStyle(folderAccent)
                Text(title)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Text(value)
                .font(.footnote.weight(.semibold))
        }
    }

    private func sectionHeader(_ title: LocalizedStringKey, icon: String) -> some View {
        Label {
            Text(title)
        } icon: {
            YabaIconView(bundleKey: icon)
                .frame(width: 20, height: 20)
                .foregroundStyle(folderAccent)
        }
    }
}
