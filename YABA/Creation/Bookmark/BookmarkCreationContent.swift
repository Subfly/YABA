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
    
    @Binding
    var initialCollection: YabaCollection?
    
    /// MARK: SHARE EXTENSION RELATED
    let link: String?
    let onExitRequested: () -> Void
    
    private let navigationTitle: String
    
    init(
        bookmarkToEdit: Binding<Bookmark?>,
        initialCollection: Binding<YabaCollection?>,
        link: String?,
        onExitRequested: @escaping () -> Void
    ) {
        _bookmarkToEdit = bookmarkToEdit
        _initialCollection = initialCollection
        self.link = link
        self.onExitRequested = onExitRequested
        
        if let _ = bookmarkToEdit.wrappedValue {
            navigationTitle = "Edit Bookmark Title"
        } else {
            navigationTitle = "Create Bookmark Title"
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
                #if !os(visionOS)
                .scrollDismissesKeyboard(.immediately)
                #endif
                .scrollContentBackground(
                    state.selectedFolder == nil
                    ? .visible
                    : .hidden
                )
            }
            .navigationTitle(LocalizedStringKey(navigationTitle))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        onExitRequested()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        onDone()
                    }.disabled(
                        state.url.isEmpty
                        || state.label.isEmpty
                        || state.selectedFolder == nil
                        || state.hasError
                    )
                }
            }
        }
        .presentationDetents([.large])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
        .tint(
            state.selectedFolder == nil
            ? .accentColor
            : state.selectedFolder?.color.getUIColor() ?? .accentColor
        )
        .onAppear(perform: onAppear)
        .overlay(
            alignment: state.toastManager.toastState.position == .top
            ? .top
            : .bottom
        ) {
            if state.toastManager.toastState.position == .top {
                YABAToast(
                    state: state.toastManager.toastState,
                    onDismissRequest: {
                        state.toastManager.hide()
                    }
                )
                .offset(y: state.toastManager.isShowing ? 75 : -1000)
                .transition(.move(edge: .top))
            } else {
                YABAToast(
                    state: state.toastManager.toastState,
                    onDismissRequest: {
                        state.toastManager.hide()
                    }
                )
                .offset(y: state.toastManager.isShowing ? -75 : 1000)
                .transition(.move(edge: .bottom))
            }
        }
        .animation(.easeInOut(duration: 0.3), value: state.toastManager.isShowing)
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
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 50, height: 50)
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
            "",
            text: $state.url,
            prompt: Text("Create Bookmark URL Placeholder")
        )
        .safeAreaInset(edge: .leading) {
            Image(systemName: "link.circle")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
        }
        
        TextField(
            "",
            text: $state.label,
            prompt: Text("Create Bookmark Title Placeholder")
        )
        .safeAreaInset(edge: .leading) {
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
                "",
                text: $state.description,
                prompt: Text("Create Bookmark Description Placeholder"),
                axis: .vertical,
            )
            .lineLimit(5, reservesSpace: true)
        }
        
        bookmarkTypePicker
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
                SelectTagsContent(selectedTags: $state.selectedTags)
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
    
    @ViewBuilder
    private var bookmarkTypePicker: some View {
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
        .id(state.selectedFolder)
    }
    
    private func onDone() {
        withAnimation {
            guard let selectedFolder = state.selectedFolder else {
                onExitRequested()
                dismiss()
                return
            }
            
            if let bookmarkToEdit {
                bookmarkToEdit.label = state.label
                bookmarkToEdit.link = state.url
                bookmarkToEdit.domain = state.host
                bookmarkToEdit.bookmarkDescription = state.description
                bookmarkToEdit.videoUrl = state.videoUrl
                bookmarkToEdit.type = state.selectedType.rawValue
                bookmarkToEdit.iconData = state.iconData
                bookmarkToEdit.imageData = state.imageData
                
                // Folder changing
                if let lastSelectedFolderIndex = bookmarkToEdit.collections.firstIndex(
                    where: { $0.collectionType == .folder }
                ) {
                    let lastSelectedFolder = bookmarkToEdit.collections[lastSelectedFolderIndex]
                    if lastSelectedFolder.hasChanges(with: selectedFolder) {
                        bookmarkToEdit.collections.remove(at: lastSelectedFolderIndex)
                        bookmarkToEdit.collections.append(selectedFolder)
                    }
                }
                
                // Tags changing
                bookmarkToEdit.collections.removeAll { $0.collectionType == .tag }
                bookmarkToEdit.collections.append(contentsOf: state.selectedTags)
            } else {
                var collections = state.selectedTags
                collections.append(selectedFolder)
                
                let newBookmark = Bookmark(
                    link: state.url,
                    label: state.label,
                    bookmarkDescription: state.description,
                    domain: state.host,
                    createdAt: .now,
                    imageData: state.imageData,
                    iconData: state.iconData,
                    videoUrl: state.videoUrl,
                    type: state.selectedType,
                    collections: collections
                )
                modelContext.insert(newBookmark)
            }
            try? modelContext.save()
            onExitRequested()
            dismiss()
        }
    }
    
    private func onAppear() {
        state.listenUrlChanges { url in
            Task { await state.fetchData(with: url) }
        }
        
        if let bookmarkToEdit {
            state.url = bookmarkToEdit.link
            state.label = bookmarkToEdit.label
            state.description = bookmarkToEdit.bookmarkDescription
            state.host = bookmarkToEdit.domain
            state.imageData = bookmarkToEdit.imageData
            state.iconData = bookmarkToEdit.iconData
            state.videoUrl = bookmarkToEdit.videoUrl
            state.selectedType = bookmarkToEdit.bookmarkType
            state.selectedFolder = bookmarkToEdit.collections.first(where: {
                $0.collectionType == .folder
            })
            state.selectedTags = bookmarkToEdit.collections.filter { $0.collectionType == .tag }
        }
        
        if let link {
            state.url = link
        }
        
        if let initialCollection {
            switch initialCollection.collectionType {
            case .folder:
                state.selectedFolder = initialCollection
            case .tag:
                state.selectedTags.append(initialCollection)
            }
        }
    }
}

#Preview {
    BookmarkCreationContent(
        bookmarkToEdit: .constant(nil),
        initialCollection: .constant(nil),
        link: nil,
        onExitRequested: {}
    )
}

#Preview {
    BookmarkCreationContent(
        bookmarkToEdit: .constant(.empty()),
        initialCollection: .constant(nil),
        link: nil,
        onExitRequested: {}
    )
}
