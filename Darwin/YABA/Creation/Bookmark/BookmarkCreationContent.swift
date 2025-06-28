//
//  BookmarkCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI
import SwiftData

struct BookmarkCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var storedContentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var storedCardImageSizing: CardViewTypeImageSizing = .small
    
    @State
    private var state: BookmarkCreationState
    private let navigationTitle: String
    
    let bookmarkToEdit: YabaBookmark?
    let collectionToFill: YabaCollection?
    
    /// MARK: SHARE EXTENSION RELATED
    let link: String?
    let onExitRequested: () -> Void
    
    init(
        bookmarkToEdit: YabaBookmark?,
        collectionToFill: YabaCollection?, // Used when creating a new bookmark to a collection
        link: String?,
        onExitRequested: @escaping () -> Void
    ) {
        self.state = .init(isInEditMode: bookmarkToEdit != nil)
        self.bookmarkToEdit = bookmarkToEdit
        self.collectionToFill = collectionToFill
        self.link = link
        self.onExitRequested = onExitRequested
        
        if bookmarkToEdit != nil {
            navigationTitle = "Edit Bookmark Title"
        } else {
            navigationTitle = "Create Bookmark Title"
        }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                if let folderColor = state.selectedFolder?.color.getUIColor() {
                    AnimatedGradient(collectionColor: folderColor)
                }
                List {
                    Section {
                        bookmarkPreviewItem
                            .redacted(reason: state.isLoading ? .placeholder : [])
                    } header: {
                        bookmarkPreviewHeader
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
                .listRowSpacing(0)
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
                        state.onDone(
                            bookmarkToEdit: bookmarkToEdit,
                            using: modelContext,
                            onFinishCallback: {
                                onExitRequested()
                                dismiss()
                            }
                        )
                    }.disabled(state.url.isEmpty || state.label.isEmpty || state.isLoading)
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
        .onAppear {
            /// MARK: BOOKMARK URL CHANGE LISTENER INITIALIZER
            state.listenUrlChanges { url in
                Task { await state.fetchData(with: url) }
            }
            Task {
                await state.onAppear(
                    link: link,
                    bookmarkToEdit: bookmarkToEdit,
                    collectionToFill: collectionToFill,
                    storedContentAppearance: storedContentAppearance,
                    storedCardImageSizing: storedCardImageSizing,
                    using: modelContext
                )
            }
        }
        .toast(
            state: state.toastManager.toastState,
            isShowing: state.toastManager.isShowing,
            onDismiss: {
                state.toastManager.hide()
            }
        )
    }
    
    @ViewBuilder
    private var bookmarkPreviewItem: some View {
        switch state.contentAppearance {
        case .list: listPreview
        case .cardSmallImage: cardSmallImagePreview
        case .cardBigImage: cardBigImagePreview
        }
    }
    
    @ViewBuilder
    private var bookmarkPreviewHeader: some View {
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
                    switch state.contentAppearance {
                    case .list:
                        state.contentAppearance = .cardSmallImage
                    case .cardSmallImage:
                        state.contentAppearance = .cardBigImage
                    case .cardBigImage:
                        state.contentAppearance = .list
                    }
                }
            } label: {
                if state.contentAppearance != .list {
                    Label {
                        Text(ViewType.card.getUITitle())
                            .textCase(.none)
                    } icon: {
                        YabaIconView(bundleKey: ViewType.card.getUIIconName())
                            .scaledToFit()
                            .frame(width: 18, height: 18)
                    }
                }
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
    
    @ViewBuilder
    private var listPreview: some View {
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
    private var cardSmallImagePreview: some View {
        VStack(alignment: .leading) {
            HStack(alignment: .center) {
                bookmarkPreviewItemImage
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                generateBookmarkItemText(
                    for: state.label,
                    localizedKey: "Bookmark Title Placeholder"
                )
                .font(.title2)
                .fontWeight(.medium)
                .lineLimit(2)
                Spacer()
            }.padding(.vertical, 4)
            
            generateBookmarkItemText(
                for: state.description,
                localizedKey: "Bookmark Description Placeholder"
            )
            .lineLimit(4)
            .multilineTextAlignment(.leading)
            .padding(.bottom, 8)
            
            HStack {
                generateTagIcons()
                Spacer()
                if let miniIconData = state.iconData,
                   let miniIconImage = UIImage(data: miniIconData) {
                    Image(uiImage: miniIconImage)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
            }
        }
    }
    
    @ViewBuilder
    private var cardBigImagePreview: some View {
        VStack(alignment: .leading) {
            bookmarkPreviewItemImage
                .clipShape(RoundedRectangle(cornerRadius: 8))
            HStack {
                if let miniIconData = state.iconData,
                   let miniIconImage = UIImage(data: miniIconData) {
                    Image(uiImage: miniIconImage)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
                generateBookmarkItemText(
                    for: state.label,
                    localizedKey: "Bookmark Title Placeholder"
                )
                .font(.title2)
                .fontWeight(.medium)
                .lineLimit(2)
            }.padding(.vertical, 4)
            
            generateBookmarkItemText(
                for: state.description,
                localizedKey: "Bookmark Description Placeholder"
            )
            .lineLimit(4)
            .multilineTextAlignment(.leading)
            .padding(.bottom, 8)
            
            HStack {
                generateTagIcons()
                Spacer()
            }
        }
    }
    
    @ViewBuilder
    private var gridPreview: some View {
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
            case .cardSmallImage:
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 50, height: 50)
            case .cardBigImage:
                RoundedRectangle(cornerRadius: 8)
                    .frame(height: 150)
                    .overlay {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .clipped()
                    }
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
                    case .grid:
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 200, height: 200)
                 */
            }
        } else {
            switch state.contentAppearance {
            case .list, .cardSmallImage:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 50, height: 50)
                    .overlay {
                        YabaIconView(bundleKey: state.selectedType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 32, height: 32)
                    }
            case .cardBigImage:
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(height: 150)
                    .overlay {
                        YabaIconView(bundleKey: state.selectedType.getIconName())
                            .scaledToFit()
                            .foregroundStyle(.tint)
                            .frame(width: 96, height: 96)
                    }
                /**
                 * TODO: OPEN WHEN LAZYVGRID IS RECYCABLE
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
                 */
            }
        }
    }
    
    @ViewBuilder
    private func generateTagIcons() -> some View {
        if !state.selectedTags.isEmpty {
            if state.selectedTags.count < 6 {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(state.selectedTags) { tag in
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
                    Spacer()
                }
            } else {
                HStack {
                    HStack(spacing: 0) {
                        ForEach(0..<5) { index in
                            let tag = state.selectedTags[index]
                            Rectangle()
                                .fill(tag.color.getUIColor().opacity(0.3))
                                .frame(width: 34, height: 34)
                                .overlay {
                                    YabaIconView(bundleKey: tag.icon)
                                        .foregroundStyle(tag.color.getUIColor())
                                        .frame(width: 24, height: 24)
                                }
                        }
                    }.clipShape(RoundedRectangle(cornerRadius: 8))
                    Text("+\(state.selectedTags.count - 5)")
                        .font(.caption)
                        .italic()
                    Spacer()
                }
            }
        } else {
            HStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(.tint.opacity(0.3))
                    .frame(width: 34, height: 34)
                    .overlay {
                        YabaIconView(bundleKey: "tags")
                            .foregroundStyle(.tint)
                            .frame(width: 24, height: 24)
                    }
                Text("Bookmark No Tags Added Title")
                    .font(.caption)
                    .italic()
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
        .disabled(state.isLoading)
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
            .disabled(state.isLoading)
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
                    if folder.collectionId == Constants.uncategorizedCollectionId {
                        Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                    } else {
                        Text(folder.label)
                    }
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
}

#Preview {
    BookmarkCreationContent(
        bookmarkToEdit: nil,
        collectionToFill: nil,
        link: nil,
        onExitRequested: {}
    )
}

#Preview {
    BookmarkCreationContent(
        bookmarkToEdit: nil,
        collectionToFill: nil,
        link: nil,
        onExitRequested: {}
    )
}
