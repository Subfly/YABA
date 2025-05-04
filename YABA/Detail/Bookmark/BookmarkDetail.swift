//
//  BookmarkDetail.swift
//  YABA
//
//  Created by Ali Taha on 29.04.2025.
//

import SwiftUI

struct BookmarkDetail: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: BookmarkDetailState = .init()
    
    @Binding
    var selectedCollection: YabaCollection?
    
    @Binding
    var bookmark: Bookmark?
    
    let onCollectionNavigationCallback: (YabaCollection) -> Void
    
    let onDeleteBookmarkCallback: (Bookmark) -> Void
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(
                collectionColor: selectedCollection?.color.getUIColor() ?? .accentColor
            )
            
            if bookmark != nil {
                #if targetEnvironment(macCatalyst)
                GeometryReader { proxy in
                    List {
                        imageSection
                        infoSection
                        folderSection
                        tagsSection
                    }
                    .scrollContentBackground(.hidden)
                    .padding(.horizontal, proxy.size.width * 0.1)
                }
                #else
                List {
                    imageSection
                    infoSection
                    folderSection
                    tagsSection
                }
                .scrollContentBackground(.hidden)
                #endif
            } else {
                ContentUnavailableView(
                    "YABA",
                    image: "UIAppIcon",
                    description: Text("YABA Description")
                )
            }
        }
        .navigationTitle(
            bookmark != nil
            ? LocalizedStringKey("Bookmark Detail Title")
            : ""
        )
        .toolbar {
            if bookmark != nil {
                optionsSection
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
                bookmarkToEdit: $bookmark,
                initialCollection: .constant(nil),
                link: nil,
                onExitRequested: {}
            )
        }
    }
    
    @ViewBuilder
    private var imageSection: some View {
        Section {
            if let imageData = self.bookmark?.imageData,
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
                if let iconData = self.bookmark?.iconData,
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
                    self.bookmark?.domain.isEmpty == true
                    ? self.bookmark?.link ?? ""
                    : self.bookmark?.domain ?? ""
                ).lineLimit(2)
            }.padding(.leading)
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        .onTapGesture {
            state.onClickOpenLink(using: bookmark)
        }
    }
    
    @ViewBuilder
    private var infoSection: some View {
        Section {
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "text")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if let label = bookmark?.label {
                    Text(label)
                        .multilineTextAlignment(.leading)
                } else {
                    Text("Bookmark Detail No Label Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                }
            }
            
            HStack(alignment: .top) {
                YabaIconView(bundleKey: "paragraph")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                if let description = bookmark?.bookmarkDescription {
                    if description.isEmpty {
                        Text("Bookmark Detail No Description Provided")
                            .foregroundStyle(.secondary)
                            .italic()
                    } else {
                        Text(description)
                            .multilineTextAlignment(.leading)
                    }
                } else {
                    Text("Bookmark Detail No Description Provided")
                        .foregroundStyle(.secondary)
                        .italic()
                }
            }
            
            HStack {
                HStack {
                    YabaIconView(bundleKey: bookmark?.bookmarkType.getIconName() ?? BookmarkType.none.getIconName())
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Create Bookmark Type Placeholder")
                }
                Spacer()
                Text(bookmark?.bookmarkType.getUITitle() ?? BookmarkType.none.getUITitle())
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
                Text(bookmark?.createdAt.formatted(date: .abbreviated, time: .shortened) ?? "")
            }
            
            if bookmark?.createdAt != bookmark?.editedAt {
                HStack {
                    HStack {
                        YabaIconView(bundleKey: "edit-02")
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                            .foregroundStyle(.tint)
                        Text("Bookmark Detail Edited At Title")
                    }
                    Spacer()
                    Text(bookmark?.editedAt.formatted(date: .abbreviated, time: .shortened) ?? "")
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
    
    @ViewBuilder
    private var folderSection: some View {
        if let collections = bookmark?.collections,
           let folder = collections.first(where: { $0.collectionType == .folder }) {
            Section {
                CollectionItemView(
                    collection: folder,
                    selectedCollection: $selectedCollection,
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
    
    @ViewBuilder
    private var tagsSection: some View {
        if let collections = bookmark?.collections {
            Section {
                let tags = collections.filter({ $0.collectionType == .tag })
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
                            selectedCollection: $selectedCollection,
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
    
    @ViewBuilder
    private var optionsSection: some View {
        #if targetEnvironment(macCatalyst)
        HStack {
            MacOSHoverableToolbarIcon(
                bundleKey: "edit-02",
                onPressed: {
                    state.shouldShowEditBookmarkSheet = true
                }
            )
            MacOSHoverableToolbarIcon(
                bundleKey: "delete-02",
                onPressed: {
                    state.shouldShowDeleteDialog = true
                }
            )
            .tint(.red)
        }
        #else
        Menu {
            Button {
                state.shouldShowEditBookmarkSheet = true
            } label: {
                Label {
                    Text("Edit")
                } icon: {
                    YabaIconView(bundleKey: "edit-02")
                        .scaledToFit()
                }
            }.tint(.orange)
            Button(role: .destructive) {
                state.shouldShowDeleteDialog = true
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

#Preview {
    BookmarkDetail(
        selectedCollection: .constant(nil),
        bookmark: .constant(nil),
        onCollectionNavigationCallback: { _ in },
        onDeleteBookmarkCallback: { _ in }
    )
}
