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
        .onDisappear {
            LinkmarkUnfurlCoordinator.shared.releaseConverterRuntime()
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
                    fallbackIcon: "link-02",
                    mainTint: mainTint
                )
                .bookmarkCreationPreviewListRowBackground(appearance: previewContentAppearance)
                .redacted(reason: machine.state.isFetchingLinkContent ? .placeholder : [])
            } header: {
                previewHeader(mainTint: mainTint)
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
                    fieldIcon("link-02", mainTint: mainTint)
                }

                if !isEditing {
                    if let cleaned = machine.state.cleanedUrl, !cleaned.isEmpty {
                        HStack {
                            fieldIcon("clean", mainTint: mainTint)
                            Text(cleaned)
                                .foregroundStyle(.secondary)
                        }
                    } else {
                        HStack {
                            fieldIcon("clean", mainTint: mainTint)
                            Text("Create Bookmark Cleaned URL Placeholder")
                                .foregroundStyle(.tertiary)
                        }
                    }
                }
            } header: {
                Label {
                    Text("Link")
                } icon: {
                    YabaIconView(bundleKey: "link-04")
                        .frame(width: 22, height: 22)
                }
            } footer: {
                if !isEditing {
                    Text("Bookmark Creation Link Info Message")
                }
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
                    metadataRow("Bookmark Creation Metadata Video URL Label", icon: "computer-video", value: machine.state.videoUrl, mainTint: mainTint)
                    metadataRow("Bookmark Creation Metadata Audio URL Label", icon: "audio-wave-01", value: machine.state.audioUrl, mainTint: mainTint)
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
                .disabled(machine.state.isSaving || machine.state.url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
    }

    private var hasApplicableMetadata: Bool {
        let title = machine.state.metadataTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let description = machine.state.metadataDescription?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return !title.isEmpty || !description.isEmpty
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
                previewImage(imageData: imageData, fallbackIcon: fallbackIcon, width: nil, height: 160, mainTint: mainTint)
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
            machine.replaceState(BookmarkFlowHydration.linkmarkUIState(from: bookmark))
            return
        }
        let resolved = BookmarkCreationFolderResolution.resolveForNewBookmark(
            modelContext: modelContext,
            preselectedFolderId: preselectedFolderId
        )
        await machine.send(
            .onInit(
                linkmarkId: nil,
                initialUrl: initialUrl,
                initialFolderId: resolved.selectedFolderId,
                initialTagIds: preselectedTagIds,
                uncategorizedFolderCreationRequired: resolved.uncategorizedFolderCreationRequired
            )
        )
    }
}
