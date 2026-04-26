//
//  DocmarkCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI
import UIKit
import UniformTypeIdentifiers
import WebKit

struct DocmarkCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var machine = DocmarkCreationStateMachine()

    @State
    private var showFolderSheet = false

    @State
    private var showTagSheet = false

    @State
    private var showFileImporter = false

    @State
    private var previewContentAppearance: PreviewContentAppearance = .list

    /// Bumps when a new document is picked so converter extraction re-runs.
    @State
    private var docExtractionGeneration: Int = 0

    let preselectedFolderId: String?
    let preselectedTagIds: [String]
    let editingBookmarkId: String?
    let onDone: () -> Void

    private var isEditing: Bool {
        editingBookmarkId != nil
    }

    var body: some View {
        NavigationStack {
            BookmarkCreationFolderVisuals(
                folderId: machine.state.selectedFolderId,
                uncategorizedCreationRequired: machine.state.uncategorizedFolderCreationRequired
            ) { folderForPresentation, mainTint in
                ZStack {
                    AnimatedGradient(color: mainTint)
                    formList(
                        mainTint: mainTint,
                        folderForPresentation: folderForPresentation
                    )
                }
            }
            .id("\(machine.state.selectedFolderId ?? "")-\(machine.state.uncategorizedFolderCreationRequired)")
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
        .fileImporter(
            isPresented: $showFileImporter,
            allowedContentTypes: [
                .pdf,
                .epub
            ],
            allowsMultipleSelection: false
        ) { result in
            Task {
                guard let url = try? result.get().first else { return }
                let scoped = url.startAccessingSecurityScopedResource()
                defer {
                    if scoped { url.stopAccessingSecurityScopedResource() }
                }
                guard let data = try? Data(contentsOf: url) else { return }
                let isPdf = url.pathExtension.lowercased() == "pdf"
                let type: DocmarkType = isPdf ? .pdf : .epub
                await machine.send(
                    .onDocumentFromShare(data, sourceFileName: url.lastPathComponent, docmarkType: type)
                )
                docExtractionGeneration += 1
            }
        }
    }

    private func formList(
        mainTint: Color,
        folderForPresentation: FolderModel?
    ) -> some View {
        List {
            Section {
                previewContent(
                    imageData: machine.state.previewImageData,
                    fallbackIcon: "doc-02",
                    mainTint: mainTint
                )
                .bookmarkCreationPreviewListRowBackground(appearance: previewContentAppearance)
                .redacted(reason: machine.state.isLoading ? .placeholder : [])
                
                Button {
                    Task { await machine.send(.onPickDocument) }
                    showFileImporter = true
                } label: {
                    Label {
                        Text("Bookmark Creation Pick Document Action")
                    } icon: {
                        YabaIconView(bundleKey: "add-circle")
                            .frame(width: 24, height: 24)
                    }
                    .bookmarkCreationActionButtonLabelStyle(
                        mainTint: mainTint,
                        isDisabled: isEditing
                    )
                }
                .buttonStyle(.plain)
                .frame(maxWidth: .infinity)
                .disabled(isEditing)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
            } header: {
                previewHeader(mainTint: mainTint)
            }

            Section {
                TextField(
                    "",
                    text: labelBinding,
                    prompt: Text("Create Bookmark Title Placeholder")
                )
                .safeAreaInset(edge: .leading) { fieldIcon("text", mainTint: mainTint) }
                TextField(
                    "",
                    text: descriptionBinding,
                    prompt: Text("Create Bookmark Description Placeholder"),
                    axis: .vertical
                )
                .lineLimit(3 ... 8)
                .safeAreaInset(edge: .leading) { fieldIcon("paragraph", mainTint: mainTint) }
                Toggle(isOn: isPinnedBinding) {
                    Label {
                        Text("Bookmark Creation Toggle Pinned Title")
                    } icon: {
                        fieldIcon(machine.state.isPinned ? "pin" : "pin-off", mainTint: mainTint)
                            .animation(.smooth, value: machine.state.isPinned)
                    }
                }

            } header: {
                HStack(spacing: 12) {
                    Label {
                        Text("Info")
                    } icon: {
                        YabaIconView(bundleKey: "information-circle")
                            .frame(width: 22, height: 22)
                    }
                    Spacer(minLength: 0)
                    if !isEditing && hasApplicableMetadata {
                        Button {
                            Task { await machine.send(.onApplyFromMetadata) }
                        } label: {
                            Text("Bookmark Creation Apply From Metadata Title")
                                .textCase(.none)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(mainTint)
                    }
                }
            }

            if hasMetadataRows {
                Section {
                    metadataRow("Bookmark Creation Metadata Title Label", icon: "text", value: machine.state.metadataTitle, mainTint: mainTint)
                    metadataRow("Bookmark Creation Metadata Description Label", icon: "paragraph", value: machine.state.metadataDescription, mainTint: mainTint)
                    metadataRow("Bookmark Creation Metadata Author Label", icon: "user-edit-01", value: machine.state.metadataAuthor, mainTint: mainTint)
                    metadataRow("Bookmark Creation Metadata Date Label", icon: "calendar-03", value: machine.state.metadataDate, mainTint: mainTint)
                } header: {
                    Label {
                        Text("Bookmark Creation Metadata Section Title")
                    } icon: {
                        YabaIconView(bundleKey: "database-01")
                            .frame(width: 22, height: 22)
                    }
                }
            }

            BookmarkFormFolderTagRows(
                folderForPresentation: folderForPresentation,
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
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
        .tint(mainTint)
        .scrollDismissesKeyboard(.immediately)
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
                .disabled(!machine.state.canSave || machine.state.isSaving)
            }
        }
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

    private var isPinnedBinding: Binding<Bool> {
        Binding(
            get: { machine.state.isPinned },
            set: { newValue in
                guard newValue != machine.state.isPinned else { return }
                Task { await machine.send(.onTogglePinned) }
            }
        )
    }

    private func previewHeader(mainTint: Color) -> some View {
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

    private var hasApplicableMetadata: Bool {
        let title = machine.state.metadataTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let description = machine.state.metadataDescription?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return !title.isEmpty || !description.isEmpty
    }

    private var hasMetadataRows: Bool {
        [
            machine.state.metadataTitle,
            machine.state.metadataDescription,
            machine.state.metadataAuthor,
            machine.state.metadataDate
        ]
        .contains { value in
            guard let value else { return false }
            return !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }
    }

    @ViewBuilder
    private func previewContent(imageData: Data?, fallbackIcon: String, mainTint: Color) -> some View {
        switch previewContentAppearance {
        case .list:
            HStack(alignment: .center, spacing: 12) {
                previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 56, height: 56, mainTint: mainTint)
                VStack(alignment: .leading, spacing: 4) {
                    if machine.state.label.isEmpty {
                        Text("Bookmark Title Placeholder")
                            .font(.headline)
                            .lineLimit(1)
                    } else {
                        Text(machine.state.label)
                            .font(.headline)
                            .lineLimit(1)
                    }
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
                    previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 56, height: 56, mainTint: mainTint)
                    if machine.state.label.isEmpty {
                        Text("Bookmark Title Placeholder")
                            .font(.headline)
                            .lineLimit(2)
                    } else {
                        Text(machine.state.label)
                            .font(.headline)
                            .lineLimit(2)
                    }
                    Spacer(minLength: 0)
                }
                if machine.state.bookmarkDescription.isEmpty {
                    Text("Bookmark Description Placeholder")
                        .foregroundStyle(.secondary)
                        .lineLimit(4)
                } else {
                    Text(machine.state.bookmarkDescription)
                        .foregroundStyle(.secondary)
                        .lineLimit(4)
                }
            }
        case .cardBigImage:
            VStack(alignment: .leading, spacing: 10) {
                previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: nil, height: 180, mainTint: mainTint)
                if machine.state.label.isEmpty {
                    Text("Bookmark Title Placeholder")
                        .font(.headline)
                } else {
                    Text(machine.state.label)
                        .font(.headline)
                }
                if machine.state.bookmarkDescription.isEmpty {
                    Text("Bookmark Description Placeholder")
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                } else {
                    Text(machine.state.bookmarkDescription)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }
            }
        case .grid:
            HStack {
                Spacer(minLength: 0)
                VStack(spacing: 0) {
                    previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: 200, height: 200, mainTint: mainTint)
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            if machine.state.label.isEmpty {
                                Text("Bookmark Title Placeholder")
                                    .font(.headline)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)
                            } else {
                                Text(machine.state.label)
                                    .font(.headline)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)
                            }
                            Spacer(minLength: 0)
                        }
                        if machine.state.bookmarkDescription.isEmpty {
                            Text("Bookmark Description Placeholder")
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                        } else {
                            Text(machine.state.bookmarkDescription)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)
                        }
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
        height: CGFloat,
        mainTint: Color
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

    private func fieldIcon(_ bundleKey: String, mainTint: Color) -> some View {
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
    private func metadataRow(
        _ key: LocalizedStringKey,
        icon: String,
        value: String?,
        mainTint: Color
    ) -> some View {
        if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            HStack(alignment: .top, spacing: 12) {
                fieldIcon(icon, mainTint: mainTint)
                    .frame(width: 24, height: 24)
                    .padding(.top, 2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(key)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(value)
                }
            }
        }
    }

    private func bootstrap() async {
        if let bid = editingBookmarkId,
           let bookmark = BookmarkFlowHydration.fetchBookmark(bookmarkId: bid, modelContext: modelContext)
        {
            machine.replaceState(BookmarkFlowHydration.docmarkUIState(from: bookmark))
            return
        }
        let resolved = BookmarkCreationFolderResolution.resolveForNewBookmark(
            modelContext: modelContext,
            preselectedFolderId: preselectedFolderId
        )
        await machine.send(
            .onInit(
                docmarkId: nil,
                initialFolderId: resolved.selectedFolderId,
                initialTagIds: preselectedTagIds,
                uncategorizedFolderCreationRequired: resolved.uncategorizedFolderCreationRequired
            )
        )
    }
}

private extension View {
    func bookmarkCreationActionButtonLabelStyle(mainTint: Color, isDisabled: Bool) -> some View {
        self
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isDisabled ? mainTint.opacity(0.45) : mainTint)
            }
            .foregroundStyle(.white)
    }
}
