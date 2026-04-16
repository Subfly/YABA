//
//  LinkmarkCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI
import UIKit

struct LinkmarkCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var machine = LinkmarkCreationStateMachine()

    @State
    private var showFolderSheet = false

    @State
    private var showTagSheet = false

    @State
    private var previewContentAppearance: PreviewContentAppearance = .list

    @Query(sort: [SortDescriptor(\FolderModel.label)])
    private var folders: [FolderModel]

    let preselectedFolderId: String?
    let preselectedTagIds: [String]
    let initialUrl: String?
    let editingBookmarkId: String?
    let onDone: () -> Void

    private var isEditing: Bool {
        editingBookmarkId != nil
    }

    var body: some View {
        NavigationStack {
            formList
        }
        .bookmarkFolderAndTagSheets(
            showFolderSheet: $showFolderSheet,
            showTagSheet: $showTagSheet,
            tagSelectionInitialIds: machine.state.selectedTagIds,
            onFolderPicked: { folderId in
                Task {
                    await machine.send(.onSelectFolderId(folderId))
                }
            },
            onTagsPicked: { ids in
                Task {
                    await machine.send(.onSelectTagIds(ids))
                }
            }
        )
        .task(id: editingBookmarkId) {
            await bootstrap()
            syncPreviewAppearanceFromMachine()
        }
    }

    private var formList: some View {
        List {
            Section {
                previewContent(
                    imageData: machine.state.previewImageData,
                    fallbackIcon: "bookmark-02"
                )
            } header: {
                previewHeader
            }

            Section {
                TextField(
                    "",
                    text: urlBinding,
                    prompt: Text("Create Bookmark URL Placeholder")
                )
                .textContentType(.URL)
                .keyboardType(.URL)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .disabled(isEditing)
                .safeAreaInset(edge: .leading) {
                    fieldIcon("link-02")
                }

                if let cleaned = machine.state.cleanedUrl, !cleaned.isEmpty {
                    HStack {
                        fieldIcon("clean")
                        Text(cleaned)
                            .foregroundStyle(.secondary)
                    }
                } else {
                    HStack {
                        fieldIcon("clean")
                        Text("Create Bookmark Cleaned URL Placeholder")
                            .foregroundStyle(.tertiary)
                    }
                }

                if machine.state.isFetchingLinkContent {
                    ProgressView()
                }
            } header: {
                Label {
                    Text("Link")
                } icon: {
                    YabaIconView(bundleKey: "link-04")
                        .frame(width: 22, height: 22)
                }
            } footer: {
                Text("Bookmark Creation Link Info Message")
            }

            Section {
                TextField(
                    "",
                    text: labelBinding,
                    prompt: Text("Create Bookmark Title Placeholder")
                )
                .safeAreaInset(edge: .leading) {
                    fieldIcon("text")
                }

                TextField(
                    "",
                    text: descriptionBinding,
                    prompt: Text("Create Bookmark Description Placeholder"),
                    axis: .vertical
                )
                .lineLimit(3 ... 8)
                .safeAreaInset(edge: .leading) {
                    fieldIcon("paragraph")
                }

                Toggle(isOn: isPrivateBinding) {
                    Label {
                        Text("Bookmark Creation Toggle Private Title")
                    } icon: {
                        fieldIcon(machine.state.isPrivate ? "circle-lock-02" : "circle-unlock-02")
                            .animation(.smooth, value: machine.state.isPrivate)
                    }
                }
                Toggle(isOn: isPinnedBinding) {
                    Label {
                        Text("Bookmark Creation Toggle Pinned Title")
                    } icon: {
                        fieldIcon(machine.state.isPinned ? "pin" : "pin-off")
                            .animation(.smooth, value: machine.state.isPinned)
                    }
                }

                if !isEditing && hasApplicableMetadata {
                    Button {
                        Task { await machine.send(.onApplyFromMetadata) }
                    } label: {
                        Label {
                            Text("Bookmark Creation Apply From Metadata Title")
                        } icon: {
                            fieldIcon("checkmark-badge-02")
                        }
                    }
                }
            } header: {
                Label {
                    Text("Info")
                } icon: {
                    YabaIconView(bundleKey: "information-circle")
                        .frame(width: 22, height: 22)
                }
            }

            if hasMetadataRows {
                Section {
                    metadataRow("Bookmark Creation Metadata Title Label", value: machine.state.metadataTitle)
                    metadataRow("Bookmark Creation Metadata Description Label", value: machine.state.metadataDescription)
                    metadataRow("Bookmark Creation Metadata Author Label", value: machine.state.metadataAuthor)
                    metadataRow("Bookmark Creation Metadata Date Label", value: machine.state.metadataDate)
                    metadataRow("Bookmark Creation Metadata Video URL Label", value: machine.state.videoUrl)
                    metadataRow("Bookmark Creation Metadata Audio URL Label", value: machine.state.audioUrl)
                } header: {
                    Label {
                        Text("Bookmark Creation Metadata Section Title")
                    } icon: {
                        YabaIconView(bundleKey: "database")
                            .frame(width: 22, height: 22)
                    }
                }
            }

            BookmarkFormFolderTagRows(
                selectedFolderId: machine.state.selectedFolderId,
                selectedTagIds: machine.state.selectedTagIds,
                onFolderNavigate: { showFolderSheet = true },
                onTagsNavigate: { showTagSheet = true }
            )

            if let lastError = machine.state.lastError {
                Section {
                    Text(lastError)
                        .foregroundStyle(.red)
                }
            }

            if let converterError = machine.state.converterError {
                Section {
                    Text(converterError)
                        .foregroundStyle(.red)
                }
            }
        }
        .tint(mainTint)
        #if !os(visionOS)
        .scrollDismissesKeyboard(.immediately)
        #endif
        .navigationTitle(
            LocalizedStringKey(isEditing ? "Edit Bookmark Title" : "Create Bookmark Title")
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(role: .cancel) {
                    dismiss()
                } label: {
                    Text("Cancel")
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button {
                    Task {
                        await machine.send(.onSave)
                        if machine.state.lastError == nil {
                            onDone()
                        }
                    }
                } label: {
                    Text("Done")
                }
                .disabled(machine.state.isSaving || machine.state.url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
    }

    private var hasApplicableMetadata: Bool {
        let title = machine.state.metadataTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let description = machine.state.metadataDescription?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return !title.isEmpty || !description.isEmpty
    }

    private var selectedFolder: FolderModel? {
        guard let selectedFolderId = machine.state.selectedFolderId else { return nil }
        return folders.first { $0.folderId == selectedFolderId }
    }

    private var mainTint: Color {
        selectedFolder?.color.getUIColor() ?? .accentColor
    }

    private var previewHeader: some View {
        HStack {
            Label {
                Text("Preview")
            } icon: {
                YabaIconView(bundleKey: "image-03")
                    .scaledToFit()
                    .frame(width: 22, height: 22)
            }
            Spacer()
            Button {
                withAnimation {
                    switch previewContentAppearance {
                    case .list:
                        previewContentAppearance = .cardSmallImage
                    case .cardSmallImage:
                        previewContentAppearance = .cardBigImage
                    case .cardBigImage:
                        previewContentAppearance = .grid
                    case .grid:
                        previewContentAppearance = .list
                    }
                }
            } label: {
                HStack(spacing: 10) {
                    if previewContentAppearance == .cardSmallImage || previewContentAppearance == .cardBigImage {
                        Label {
                            Text(ContentAppearance.card.getUITitle())
                                .textCase(.none)
                        } icon: {
                            YabaIconView(bundleKey: ContentAppearance.card.getUIIconName())
                                .scaledToFit()
                                .frame(width: 22, height: 22)
                        }
                    }
                    Label {
                        Text(previewContentAppearance.getUITitle())
                            .textCase(.none)
                    } icon: {
                        YabaIconView(bundleKey: previewContentAppearance.getUIIconName())
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                    }
                }
            }
            .buttonStyle(.plain)
            .foregroundStyle(mainTint)
        }
    }

    private var hasMetadataRows: Bool {
        [
            machine.state.metadataTitle,
            machine.state.metadataDescription,
            machine.state.metadataAuthor,
            machine.state.metadataDate,
            machine.state.videoUrl,
            machine.state.audioUrl
        ]
        .contains { value in
            guard let value else { return false }
            return !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    private var urlBinding: Binding<String> {
        Binding(
            get: { machine.state.url },
            set: { newValue in
                Task {
                    await machine.send(.onChangeUrl(newValue))
                }
            }
        )
    }

    private var labelBinding: Binding<String> {
        Binding(
            get: { machine.state.label },
            set: { newValue in
                Task {
                    await machine.send(.onChangeLabel(newValue))
                }
            }
        )
    }

    private var descriptionBinding: Binding<String> {
        Binding(
            get: { machine.state.bookmarkDescription },
            set: { newValue in
                Task {
                    await machine.send(.onChangeDescription(newValue))
                }
            }
        )
    }

    private var isPrivateBinding: Binding<Bool> {
        Binding(
            get: { machine.state.isPrivate },
            set: { newValue in
                guard newValue != machine.state.isPrivate else { return }
                Task { await machine.send(.onTogglePrivate) }
            }
        )
    }

    private var isPinnedBinding: Binding<Bool> {
        Binding(
            get: { machine.state.isPinned },
            set: { newValue in
                guard newValue != machine.state.isPinned else { return }
                Task { await machine.send(.onTogglePinned) }
            }
        )
    }

    @ViewBuilder
    private func previewContent(imageData: Data?, fallbackIcon: String) -> some View {
        switch previewContentAppearance {
        case .list:
            HStack(alignment: .center, spacing: 12) {
                previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 56, height: 56)
                VStack(alignment: .leading, spacing: 4) {
                    Text(machine.state.label.isEmpty ? "Bookmark Title Placeholder" : machine.state.label)
                        .font(.headline)
                        .lineLimit(1)
                    if machine.state.bookmarkDescription.isEmpty {
                        Text("Bookmark Description Placeholder")
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    } else {
                        Text(machine.state.bookmarkDescription)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
            }
        case .cardSmallImage:
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .center, spacing: 10) {
                    previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 56, height: 56)
                    Text(machine.state.label.isEmpty ? "Bookmark Title Placeholder" : machine.state.label)
                        .font(.headline)
                        .lineLimit(2)
                    Spacer(minLength: 0)
                }
                Text(machine.state.bookmarkDescription.isEmpty ? "Bookmark Description Placeholder" : machine.state.bookmarkDescription)
                    .foregroundStyle(.secondary)
                    .lineLimit(4)
            }
        case .cardBigImage:
            VStack(alignment: .leading, spacing: 10) {
                previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: nil, height: 160)
                Text(machine.state.label.isEmpty ? "Bookmark Title Placeholder" : machine.state.label)
                    .font(.headline)
                Text(machine.state.bookmarkDescription.isEmpty ? "Bookmark Description Placeholder" : machine.state.bookmarkDescription)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
            }
        case .grid:
            HStack {
                Spacer(minLength: 0)
                VStack(spacing: 0) {
                    previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 200, height: 200)
                    HStack {
                        Text(machine.state.label.isEmpty ? "Bookmark Title Placeholder" : machine.state.label)
                            .font(.headline)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        Spacer(minLength: 0)
                    }
                    .padding()
                }
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.thinMaterial)
                }
                .frame(width: 200)
                Spacer(minLength: 0)
            }
        }
    }

    @ViewBuilder
    private func previewImage(
        imageData: Data?,
        fallbackIcon: String,
        width: CGFloat?,
        height: CGFloat
    ) -> some View {
        if let imageData, let image = UIImage(data: imageData) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: width, height: height)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 10))
        } else {
            RoundedRectangle(cornerRadius: 10)
                .fill(mainTint.opacity(0.25))
                .frame(width: width, height: height)
                .overlay {
                    YabaIconView(bundleKey: fallbackIcon)
                        .frame(width: 28, height: 28)
                        .foregroundStyle(mainTint)
                }
        }
    }

    private func fieldIcon(_ bundleKey: String) -> some View {
        YabaIconView(bundleKey: bundleKey)
            .scaledToFit()
            .frame(width: 24, height: 24)
            .foregroundStyle(mainTint)
    }

    private func syncPreviewAppearanceFromMachine() {
        switch machine.state.bookmarkAppearance {
        case .list:
            previewContentAppearance = .list
        case .card:
            previewContentAppearance = machine.state.cardImageSizing == .big ? .cardBigImage : .cardSmallImage
        case .grid:
            previewContentAppearance = .grid
        }
    }

    @ViewBuilder
    private func metadataRow(_ key: LocalizedStringKey, value: String?) -> some View {
        if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            VStack(alignment: .leading, spacing: 2) {
                Text(key)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(value)
            }
        }
    }

    private func bootstrap() async {
        if let bid = editingBookmarkId,
           let bookmark = BookmarkFlowHydration.fetchBookmark(bookmarkId: bid, modelContext: modelContext)
        {
            machine.replaceState(BookmarkFlowHydration.linkmarkUIState(from: bookmark))
            return
        }
        await machine.send(
            .onInit(
                linkmarkId: nil,
                initialUrl: initialUrl,
                initialFolderId: preselectedFolderId,
                initialTagIds: preselectedTagIds
            )
        )
    }
}
