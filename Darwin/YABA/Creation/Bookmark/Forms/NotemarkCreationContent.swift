//
//  NotemarkCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI

struct NotemarkCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var machine = NotemarkCreationStateMachine()

    @State
    private var showFolderSheet = false

    @State
    private var showTagSheet = false

    @State
    private var previewContentAppearance: PreviewContentAppearance = .list

    @State
    private var privatePinSheet: PrivateBookmarkPinRoute?

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
                    #if !targetEnvironment(macCatalyst)
                    AnimatedGradient(color: mainTint)
                    #endif
                    formList(
                        mainTint: mainTint,
                        folderForPresentation: folderForPresentation,
                        privatePinSheet: $privatePinSheet
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
        .sheet(item: $privatePinSheet) { route in
            switch route {
            case .create:
                BookmarkPasswordCreateSheet()
            case let .entry(bookmarkId, reason):
                BookmarkPasswordEntrySheet(bookmarkId: bookmarkId, reason: reason)
            }
        }
        .task(id: editingBookmarkId) {
            await bootstrap()
            syncPreviewAppearanceFromMachine()
        }
    }

    private func formList(
        mainTint: Color,
        folderForPresentation: FolderModel?,
        privatePinSheet: Binding<PrivateBookmarkPinRoute?>
    ) -> some View {
        List {
            Section {
                previewContent(fallbackIcon: "note-edit", mainTint: mainTint)
                    .bookmarkCreationPreviewListRowBackground(appearance: previewContentAppearance)
            } header: {
                previewHeader(mainTint: mainTint)
            }

            Section {
                TextField(
                    "",
                    text: labelBinding,
                    prompt: Text("Create Bookmark Title Placeholder")
                )
                .safeAreaInset(edge: .leading) {
                    fieldIcon("text", mainTint: mainTint)
                }
                TextField(
                    "",
                    text: descriptionBinding,
                    prompt: Text("Create Bookmark Description Placeholder"),
                    axis: .vertical
                )
                .lineLimit(3 ... 8)
                .safeAreaInset(edge: .leading) {
                    fieldIcon("paragraph", mainTint: mainTint)
                }
                Toggle(isOn: privateToggleBinding(privatePinSheet: privatePinSheet)) {
                    Label {
                        Text("Bookmark Creation Toggle Private Title")
                    } icon: {
                        fieldIcon(machine.state.isPrivate ? "circle-lock-02" : "circle-unlock-02", mainTint: mainTint)
                            .animation(.smooth, value: machine.state.isPrivate)
                    }
                }
                Toggle(isOn: isPinnedBinding) {
                    Label {
                        Text("Bookmark Creation Toggle Pinned Title")
                    } icon: {
                        fieldIcon(machine.state.isPinned ? "pin" : "pin-off", mainTint: mainTint)
                            .animation(.smooth, value: machine.state.isPinned)
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
                .disabled(machine.state.isSaving || machine.state.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
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

    private func privateToggleBinding(privatePinSheet: Binding<PrivateBookmarkPinRoute?>) -> Binding<Bool> {
        Binding(
            get: { machine.state.isPrivate },
            set: { newValue in
                guard newValue != machine.state.isPrivate else { return }
                Task {
                    await PrivateBookmarkCreationPinGate.run(pinSheet: privatePinSheet) {
                        await machine.send(.onTogglePrivate)
                    }
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
                YabaIconView(bundleKey: "note-edit")
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

    @ViewBuilder
    private func previewContent(fallbackIcon: String, mainTint: Color) -> some View {
        switch previewContentAppearance {
        case .list:
            HStack(alignment: .center, spacing: 12) {
                placeholderPreviewImage(fallbackIcon: fallbackIcon, width: 56, height: 56, mainTint: mainTint)
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
                    placeholderPreviewImage(fallbackIcon: fallbackIcon, width: 56, height: 56, mainTint: mainTint)
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
                placeholderPreviewImage(fallbackIcon: fallbackIcon, width: nil, height: 160, mainTint: mainTint)
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
                    placeholderPreviewImage(fallbackIcon: fallbackIcon, width: 200, height: 200, mainTint: mainTint)
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

    private func placeholderPreviewImage(fallbackIcon: String, width: CGFloat?, height: CGFloat, mainTint: Color) -> some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(mainTint.opacity(0.25))
            .frame(width: width, height: height)
            .overlay {
                YabaIconView(bundleKey: fallbackIcon)
                    .frame(width: 28, height: 28)
                    .foregroundStyle(mainTint)
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

    private func bootstrap() async {
        if let bid = editingBookmarkId,
           let bookmark = BookmarkFlowHydration.fetchBookmark(bookmarkId: bid, modelContext: modelContext)
        {
            machine.replaceState(BookmarkFlowHydration.notemarkUIState(from: bookmark))
            return
        }
        let resolved = BookmarkCreationFolderResolution.resolveForNewBookmark(
            modelContext: modelContext,
            preselectedFolderId: preselectedFolderId
        )
        await machine.send(
            .onInit(
                notemarkId: nil,
                initialFolderId: resolved.selectedFolderId,
                initialTagIds: preselectedTagIds,
                uncategorizedFolderCreationRequired: resolved.uncategorizedFolderCreationRequired
            )
        )
    }
}
