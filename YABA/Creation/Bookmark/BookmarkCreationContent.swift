//
//  BookmarkCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI

struct BookmarkCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: BookmarkCreationState = .init()
    
    @Binding
    var bookmarkToEdit: Bookmark?
    
    private let navigationTitle: String
    
    init(bookmarkToEdit: Binding<Bookmark?>) {
        _bookmarkToEdit = bookmarkToEdit
        if bookmarkToEdit.wrappedValue == nil {
            navigationTitle = "Create Bookmark Title"
        } else {
            navigationTitle = "Edit Bookmark Title"
        }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                if let folderColor = state.selectedFolder?.color.getUIColor() {
                    AnimatedMeshGradient(collectionColor: folderColor)
                }
                Form {
                    Section {
                        bookmarkPreviewItem
                            .redacted(reason: state.isLoading ? .placeholder : [])
                    } header: {
                        Label(
                            "Preview",
                            systemImage: "rectangle.and.text.magnifyingglass"
                        )
                    }
                    
                    Section {
                        bookmarkInfoSectionItems
                    } header: {
                        Label(
                            "Info",
                            systemImage: "info.circle"
                        )
                    }

                    Section {
                        bookmarkFolderSelectionView
                    } header: {
                        Label(
                            "Folder",
                            systemImage: "folder"
                        )
                    }
                    
                    Section {
                        bookmarkAddTagsView
                    } header: {
                        bookmarkAddOrEditTagsButton
                    }
                }
                .scrollDismissesKeyboard(.immediately)
                .scrollContentBackground(
                    state.selectedFolder == nil
                    ? .visible
                    : .hidden
                )
            }
            .navigationTitle(LocalizedStringKey(navigationTitle))
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .tint(
            state.selectedFolder == nil
            ? .accentColor
            : state.selectedFolder?.color.getUIColor() ?? .accentColor
        )
        .onAppear {
            state.listenUrlChanges { url in
                Task {
                    await state.fetchData(with: url)
                }
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkPreviewItem: some View {
        HStack(alignment: .center) {
            bookmarkPreviewItemImage
                .clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading) {
                generateBookmarkItemText(
                    for: state.label,
                    localizedKey: "Bookmark Title Placeholder"
                )
                .font(.title3)
                .fontWeight(.medium)
                .lineLimit(1)
                generateBookmarkItemText(
                    for: state.description,
                    localizedKey: "Bookmark Description Placeholder"
                ).lineLimit(2)
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkPreviewItemImage: some View {
        if let imageData = state.imageData,
           let image = UIImage(data: imageData) {
            #if os(iOS)
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 50, height: 50)
            #elseif os(macOS)
            Image(nsImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 50, height: 50)
            #endif
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(.tint.opacity(0.3))
                .frame(width: 64, height: 64)
                .overlay {
                    Image(systemName: state.selectedType.getIconName())
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .foregroundStyle(.tint)
                        .frame(width: 32, height: 32)
                }
        }
    }
    
    @ViewBuilder
    private func generateBookmarkItemText(
        for text: String,
        localizedKey: String
    ) -> some View {
        if text.isEmpty {
            Text(LocalizedStringKey(localizedKey))
        } else {
            Text(text)
        }
    }
    
    @ViewBuilder
    private var bookmarkInfoSectionItems: some View {
        TextField(
            "Create Bookmark URL Placeholder",
            text: $state.url,
            prompt: Text("Create Bookmark URL Placeholder")
        ).safeAreaInset(edge: .leading) {
            Image(systemName: "link.circle")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
        }
        TextField(
            "Create Bookmark Title Placeholder",
            text: $state.label,
            prompt: Text("Create Bookmark Title Placeholder")
        ).safeAreaInset(edge: .leading) {
            Image(systemName: "t.square")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
        }
        HStack(alignment: .top) {
            Image(systemName: "text.page")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
            TextField(
                "Create Bookmark Description Placeholder",
                text: $state.description,
                prompt: Text("Create Bookmark Description Placeholder"),
                axis: .vertical,
            )
            .lineLimit(5, reservesSpace: true)
        }
        Picker(selection: $state.selectedType) {
            ForEach(BookmarkType.allCases, id: \.self) { type in
                Label(type.getUITitle(), systemImage: type.getIconName())
            }
        } label: {
            HStack {
                Image(systemName: state.selectedType.getIconName())
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
                Text("Create Bookmark Type Placeholder")
            }
        }
        .foregroundStyle(.tint)
    }
    
    @ViewBuilder
    private var bookmarkFolderSelectionView: some View {
        NavigationLink {
            SelectFolderContent(selectedFolder: $state.selectedFolder)
        } label: {
            if let folder = state.selectedFolder {
                HStack {
                    Image(systemName: folder.icon)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text(folder.label)
                }
            } else {
                HStack {
                    Image(systemName: "folder")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text("Create Bookmark Folder Placeholder")
                }
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkAddTagsView: some View {
        if state.selectedTags.isEmpty {
            ContentUnavailableView(
                "Create Bookmark No Tags Selected Title",
                systemImage: "tag",
                description: Text("Create Bookmark No Tags Selected Description")
            )
        } else {
            ForEach(state.selectedTags) { tag in
                HStack {
                    Image(systemName: tag.icon)
                        .foregroundStyle(tag.color.getUIColor())
                    Text(tag.label)
                }
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkAddOrEditTagsButton: some View {
        HStack {
            Label(
                "Tags Title",
                systemImage: "tag"
            )
            Spacer()
            NavigationLink {
                
            } label: {
                Label(
                    state.selectedTags.isEmpty
                    ? "Create Bookmark Add Tags"
                    : "Create Bookmark Edit Tags",
                    systemImage: state.selectedTags.isEmpty
                    ? "plus"
                    : "pencil"
                ).textCase(.none)
            }
        }
    }
}

#Preview {
    BookmarkCreationContent(bookmarkToEdit: .constant(nil))
}

#Preview {
    BookmarkCreationContent(bookmarkToEdit: .constant(.empty()))
}
