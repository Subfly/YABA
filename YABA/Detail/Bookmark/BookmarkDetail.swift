//
//  BookmarkDetail.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import SwiftUI

struct GeneralBookmarkDetail: View {
    @Environment(\.appState)
    private var appState
    
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (Bookmark) -> Void
    
    var body: some View {
        BookmarkDetail(
            bookmark: appState.selectedBookmark,
            onCollectionNavigationCallback: onCollectionNavigationCallback,
            onDeleteBookmarkCallback: { bookmark in
                appState.selectedBookmark = nil
                onDeleteBookmarkCallback(bookmark)
            }
        )
    }
}

struct MobileBookmarkDetail: View {
    let bookmark: Bookmark?
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (Bookmark) -> Void
    
    var body: some View {
        BookmarkDetail(
            bookmark: bookmark,
            onCollectionNavigationCallback: onCollectionNavigationCallback,
            onDeleteBookmarkCallback: onDeleteBookmarkCallback
        )
    }
}

private struct BookmarkDetail: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: BookmarkDetailState = .init()
    
    let bookmark: Bookmark?
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    let onDeleteBookmarkCallback: (Bookmark) -> Void
    
    var body: some View {
        let _ = Self._printChanges()
        ZStack {
            AnimatedMeshGradient(collectionColor: state.meshColor)
            
            if let bookmark {
                #if targetEnvironment(macCatalyst)
                GeometryReader { proxy in
                    MainContent(
                        bookmark: bookmark,
                        folder: state.folder,
                        tags: state.tags,
                        onClickOpenLink: {
                            state.onClickOpenLink(using: bookmark)
                        },
                        onCollectionNavigationCallback: onCollectionNavigationCallback
                    )
                    .padding(.horizontal, proxy.size.width * 0.1)
                }
                #else
                MainContent(
                    bookmark: bookmark,
                    onClickOpenLink: {
                        state.onClickOpenLink(using: appState.selectedBookmark)
                    },
                    onCollectionNavigationCallback: onCollectionNavigationCallback
                )
                #endif
            } else {
                ContentUnavailableView(
                    "YABA",
                    image: "UIAppIcon",
                    description: Text("YABA Description")
                )
            }
        }
        .tint(state.meshColor)
        .navigationTitle(
            bookmark != nil
            ? LocalizedStringKey("Bookmark Detail Title")
            : ""
        )
        .toolbar {
            if bookmark != nil {
                OptionItems(
                    shouldShowEditBookmarkSheet: $state.shouldShowEditBookmarkSheet,
                    shouldShowDeleteDialog: $state.shouldShowDeleteDialog
                )
            }
        }
        .alert(
            LocalizedStringKey("Delete Bookmark Title"),
            isPresented: $state.shouldShowDeleteDialog,
        ) {
            alertActionItems
        } message: {
            if let bookmark {
                Text("Delete Content Message \(bookmark.label)")
            }
        }
        .sheet(isPresented: $state.shouldShowEditBookmarkSheet) {
            BookmarkCreationContent(
                bookmarkToEdit: bookmark,
                collectionToFill: nil,
                link: nil,
                onExitRequested: {}
            )
        }
        .onAppear {
            state.initialize(with: bookmark)
        }
    }
    
    @ViewBuilder
    private var alertActionItems: some View {
        Button(role: .cancel) {
            state.shouldShowDeleteDialog = false
        } label: {
            Text("Cancel")
        }
        Button(role: .destructive) {
            withAnimation {
                if let bookmark {
                    modelContext.delete(bookmark)
                    try? modelContext.save()
                    state.shouldShowDeleteDialog = false
                    onDeleteBookmarkCallback(bookmark)
                }
            }
        } label: {
            Text("Delete")
        }
    }
}

private struct MainContent: View {
    let bookmark: Bookmark
    let folder: YabaCollection?
    let tags: [YabaCollection]
    let onClickOpenLink: () -> Void
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        List {
            ImageSection(
                bookmark: bookmark,
                onClickOpenLink: onClickOpenLink
            )
            InfoSection(bookmark: bookmark)
            if let folder {
                FolderSection(
                    folder: folder,
                    onCollectionNavigationCallback: onCollectionNavigationCallback
                )
            }
            TagsSection(
                tags: tags,
                onCollectionNavigationCallback: onCollectionNavigationCallback
            )
        }
        .scrollContentBackground(.hidden)
    }
}

private struct ImageSection: View {
    let bookmark: Bookmark
    let onClickOpenLink: () -> Void
    
    var body: some View {
        Section {
            if let imageData = bookmark.imageData,
               let image = UIImage(data: imageData) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(idealHeight: 250, alignment: .center)
            } else {
                ContentUnavailableView {
                    Label {
                        Text("Bookmark Detail Image Error Title")
                    } icon: {
                        YabaIconView(bundleKey: "image-not-found-01")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("Bookmark Detail Image Error Description")
                }
            }
        } header: {
            Label {
                Text("Bookmark Detail Image Header Title")
            } icon: {
                YabaIconView(bundleKey: "image-03")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }.padding(.leading)

        } footer: {
            HStack {
                if let iconData = bookmark.iconData,
                   let image = UIImage(data: iconData) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                } else {
                    YabaIconView(bundleKey: "link-02")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                Text(
                    bookmark.domain.isEmpty
                    ? bookmark.link
                    : bookmark.domain
                ).lineLimit(2)
            }.padding(.leading)
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        .onTapGesture {
            onClickOpenLink()
        }
    }
}

private struct InfoSection: View {
    let bookmark: Bookmark
    
    var body: some View {
        Section {
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "text")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                Text(bookmark.label)
                    .multilineTextAlignment(.leading)
            }
            
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "paragraph")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if bookmark.bookmarkDescription.isEmpty {
                    Text("Bookmark Detail No Description Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                } else {
                    Text(bookmark.bookmarkDescription)
                        .multilineTextAlignment(.leading)
                }
            }
            
            HStack {
                HStack {
                    YabaIconView(bundleKey: bookmark.bookmarkType.getIconName())
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Create Bookmark Type Placeholder")
                }
                Spacer()
                Text(bookmark.bookmarkType.getUITitle())
            }
            
            HStack {
                HStack {
                    YabaIconView(bundleKey: "clock-01")
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Bookmark Detail Created At Title")
                }
                Spacer()
                Text(bookmark.createdAt.formatted(date: .abbreviated, time: .shortened))
            }
            
            if bookmark.createdAt != bookmark.editedAt {
                HStack {
                    HStack {
                        YabaIconView(bundleKey: "edit-02")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundStyle(.tint)
                        Text("Bookmark Detail Edited At Title")
                    }
                    Spacer()
                    Text(bookmark.editedAt.formatted(date: .abbreviated, time: .shortened))
                }
            }
        } header: {
            Label {
                Text("Info")
            } icon: {
                YabaIconView(bundleKey: "information-circle")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct FolderSection: View {
    let folder: YabaCollection
    let onCollectionNavigationCallback: (YabaCollection) -> Void

    var body: some View {
        Section {
            CollectionItemView(
                collection: folder,
                isInSelectionMode: false,
                isInBookmarkDetail: true,
                onDeleteCallback: { _ in },
                onEditCallback: { _ in },
                onNavigationCallback: onCollectionNavigationCallback
            )
        } header: {
            Label {
                Text("Folder")
            } icon: {
                YabaIconView(bundleKey: "folder-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct TagsSection: View {
    let tags: [YabaCollection]
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        Section {
            if tags.isEmpty {
                ContentUnavailableView {
                    Label {
                        Text("Bookmark Detail No Tags Added Title")
                    } icon: {
                        YabaIconView(bundleKey: "tags")
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                    }
                } description: {
                    Text("Bookmark Detail No Tags Added Description")
                }
            } else {
                ForEach(tags) { tag in
                    CollectionItemView(
                        collection: tag,
                        isInSelectionMode: false,
                        isInBookmarkDetail: true,
                        onDeleteCallback: { _ in },
                        onEditCallback: { _ in },
                        onNavigationCallback: onCollectionNavigationCallback
                    )
                }
            }
        } header: {
            Label {
                Text("Tags Title")
            } icon: {
                YabaIconView(bundleKey: "tag-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct OptionItems: View {
    @Binding
    var shouldShowEditBookmarkSheet: Bool
    
    @Binding
    var shouldShowDeleteDialog: Bool
    
    var body: some View {
        #if targetEnvironment(macCatalyst)
        HStack(spacing: 0) {
            MacOSHoverableToolbarIcon(
                bundleKey: "edit-02",
                onPressed: {
                    shouldShowEditBookmarkSheet = true
                }
            )
            .tint(.orange)
            MacOSHoverableToolbarIcon(
                bundleKey: "delete-02",
                onPressed: {
                    shouldShowDeleteDialog = true
                }
            )
            .tint(.red)
        }
        #else
        Menu {
            Button {
                shouldShowEditBookmarkSheet = true
            } label: {
                Label {
                    Text("Edit")
                } icon: {
                    YabaIconView(bundleKey: "edit-02")
                        .scaledToFit()
                }
            }.tint(.orange)
            Button(role: .destructive) {
                shouldShowDeleteDialog = true
            } label: {
                Label {
                    Text("Delete")
                } icon: {
                    YabaIconView(bundleKey: "delete-02")
                        .scaledToFit()
                }
            }.tint(.red)
        } label: {
            YabaIconView(bundleKey: "more-horizontal-circle-02")
                .scaledToFit()
        }
        #endif
    }
}

#Preview {
    BookmarkDetail(
        bookmark: .empty(),
        onCollectionNavigationCallback: { _ in },
        onDeleteBookmarkCallback: { _ in }
    )
}
