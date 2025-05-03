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
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var storedContentAppearance: ViewType = .list
    
    @State
    private var state: BookmarkCreationState
    
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
        
        self.state = .init(isInEditMode: bookmarkToEdit.wrappedValue != nil)
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
                        if state.contentAppearance == .list {
                            bookmarkPreviewItem
                                .redacted(reason: state.isLoading ? .placeholder : [])
                        } else {
                            bookmarkPreviewItem
                                .redacted(reason: state.isLoading ? .placeholder : [])
                                .listRowBackground(Color.clear)
                        }
                    } header: {
                        HStack {
                            Label {
                                Text("Preview")
                            } icon: {
                                YabaIconView(bundleKey: "image-03")
                                    .scaledToFit()
                                    .frame(width: 18, height: 18)
                            }
                            Spacer()
                            Button {
                                withAnimation {
                                    if state.contentAppearance == .list {
                                        state.contentAppearance = .grid
                                    } else {
                                        state.contentAppearance = .list
                                    }
                                }
                            } label: {
                                Label {
                                    Text(state.contentAppearance.getUITitle())
                                        .textCase(.none)
                                } icon: {
                                    YabaIconView(bundleKey: state.contentAppearance.getUIIconName())
                                        .scaledToFit()
                                        .frame(width: 18, height: 18)
                                }
                            }
                        }
                    }
                    
                    Section {
                        bookmarkInfoSectionItems
                    } header: {
                        Label {
                            Text("Info")
                        } icon: {
                            YabaIconView(bundleKey: "information-circle")
                                .scaledToFit()
                                .frame(width: 18, height: 18)
                        }
                    }
                    
                    Section {
                        bookmarkFolderSelectionView
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
                YabaToast(
                    state: state.toastManager.toastState,
                    onDismissRequest: {
                        state.toastManager.hide()
                    }
                )
                .offset(y: state.toastManager.isShowing ? 25 : -1000)
                .transition(.move(edge: .top))
            } else {
                YabaToast(
                    state: state.toastManager.toastState,
                    onDismissRequest: {
                        state.toastManager.hide()
                    }
                )
                .offset(y: state.toastManager.isShowing ? -25 : 1000)
                .transition(.move(edge: .bottom))
            }
        }
        .animation(.easeInOut(duration: 0.3), value: state.toastManager.isShowing)
    }
    
    @ViewBuilder
    private var bookmarkPreviewItem: some View {
        switch state.contentAppearance {
        case .list:
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
        case .grid:
            HStack {
                Spacer()
                VStack(spacing: 0) {
                    bookmarkPreviewItemImage
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    HStack {
                        generateBookmarkItemText(
                            for: state.label,
                            localizedKey: "Bookmark Title Placeholder"
                        )
                        .font(.title3)
                        .fontWeight(.medium)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                        Spacer()
                    }.padding()
                }
                .background {
                    RoundedRectangle(cornerRadius: 12)
                    #if targetEnvironment(macCatalyst)
                        .fill(.gray.opacity(0.1))
                    #else
                        .fill(.thinMaterial)
                    #endif
                }
                .frame(width: 200)
                Spacer()
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkPreviewItemImage: some View {
        if let imageData = state.imageData,
           let image = UIImage(data: imageData) {
            switch state.contentAppearance {
            case .list:
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 50, height: 50)
            case .grid:
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 200, height: 200)
            }
        } else {
            switch state.contentAppearance {
            case .list:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: state.selectedType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 32, height: 32)
                    }
            case .grid:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 200, height: 200)
                    .overlay {
                        YabaIconView(bundleKey: state.selectedType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 92, height: 92)
                    }
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
            YabaIconView(bundleKey: "link-02")
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
        }
        
        TextField(
            "",
            text: $state.label,
            prompt: Text("Create Bookmark Title Placeholder")
        )
        .safeAreaInset(edge: .leading) {
            YabaIconView(bundleKey: "text")
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.tint)
        }
        
        HStack(alignment: .top) {
            YabaIconView(bundleKey: "paragraph")
                .scaledToFit()
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
                    YabaIconView(bundleKey: folder.icon)
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .foregroundStyle(.tint)
                    Text(folder.label)
                }
            } else {
                HStack {
                    YabaIconView(bundleKey: "folder-01")
                        .scaledToFit()
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
            ForEach(state.selectedTags) { tag in
                HStack {
                    YabaIconView(bundleKey: tag.icon)
                        .frame(width: 24, height: 24)
                        .foregroundStyle(tag.color.getUIColor())
                    Text(tag.label)
                }
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkAddOrEditTagsButton: some View {
        HStack {
            Label {
                Text("Tags Title")
            } icon: {
                YabaIconView(bundleKey: "tag-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
            Spacer()
            NavigationLink {
                SelectTagsContent(selectedTags: $state.selectedTags)
            } label: {
                Label {
                    Text(
                        LocalizedStringKey(
                            state.selectedTags.isEmpty
                            ? "Create Bookmark Add Tags"
                            : "Create Bookmark Edit Tags"
                        )
                    ).textCase(.none)
                } icon: {
                    YabaIconView(
                        bundleKey: state.selectedTags.isEmpty
                        ? "plus-sign"
                        : "edit-02"
                    )
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                }
            }
        }
    }
    
    @ViewBuilder
    private var bookmarkTypePicker: some View {
        Picker(selection: $state.selectedType) {
            ForEach(BookmarkType.allCases, id: \.self) { type in
                Label {
                    // TODO: ADD TO LOCALIZATION
                    Text(type.getUITitle())
                } icon: {
                    YabaIconView(bundleKey: type.getIconName())
                        .scaledToFit()
                }
            }
        } label: {
            HStack {
                YabaIconView(bundleKey: state.selectedType.getIconName())
                    .scaledToFit()
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
                bookmarkToEdit.editedAt = .now
                
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
                    editedAt: .now,
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
        state.contentAppearance = storedContentAppearance
        
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
