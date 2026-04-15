//
//  FolderCreationContent.swift
//  YABA
//
//  Create / edit folder — parity with Compose `FolderCreationContent`, driven by `FolderCreationStateMachine`.
//

import SwiftData
import SwiftUI

struct FolderCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var machine = FolderCreationStateMachine(
        initialState: FolderCreationUIState(colorRole: .blue)
    )

    @State
    private var shouldShowIconPicker = false
    @State
    private var shouldShowColorPicker = false
    @State
    private var shouldShowParentPicker = false

    /// When non-`nil`, hydrates from SwiftData for edit flows.
    private let existingFolderId: String?

    init(existingFolderId: String? = nil) {
        self.existingFolderId = existingFolderId
    }

    private var navigationTitleKey: String {
        existingFolderId == nil ? "Create Folder Title" : "Edit Folder Title"
    }

    private var canSave: Bool {
        !machine.state.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                HStack(alignment: .center, spacing: 24) {
                    iconPickerButton
                    labelField
                    colorPickerButton
                }
                .padding(.horizontal)
                .padding(.horizontal)

                descriptionField
                    .padding(.horizontal)

                parentFolderRow
                    .padding(.horizontal)
            }
            .padding(.vertical)
            .navigationTitle(Text(LocalizedStringKey(navigationTitleKey)))
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
                            dismiss()
                        }
                    } label: {
                        Text("Done")
                    }
                    .disabled(!canSave)
                }
            }
            .sheet(isPresented: $shouldShowIconPicker) {
                IconPickerView(
                    currentSelectedIcon: machine.state.icon,
                    onSelectIcon: { newName in
                        Task {
                            await machine.send(.onSelectNewIcon(newName))
                            shouldShowIconPicker = false
                        }
                    },
                    onCancel: {
                        shouldShowIconPicker = false
                    }
                )
            }
            .sheet(isPresented: $shouldShowColorPicker) {
                YabaColorPicker(
                    selection: Binding(
                        get: { machine.state.colorRole },
                        set: { new in
                            Task {
                                await machine.send(.onSelectNewColor(new))
                            }
                        }
                    ),
                    onDismiss: {
                        shouldShowColorPicker = false
                    }
                )
            }
            .sheet(isPresented: $shouldShowParentPicker) {
                NavigationStack {
                    SelectFolderContent(
                        mode: .parentSelection,
                        contextFolderId: existingFolderId,
                        onPick: { id in
                            Task {
                                await machine.send(.onSelectNewParent(parentFolderId: id))
                            }
                        }
                    )
                }
            }
        }
        .presentationDetents([.medium])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
        .task(id: existingFolderId) {
            await hydrateIfNeeded()
        }
    }

    private var iconPickerButton: some View {
        Button {
            shouldShowIconPicker = true
        } label: {
            YabaIconView(bundleKey: machine.state.icon)
                .aspectRatio(contentMode: .fit)
                .foregroundStyle(machine.state.colorRole.getUIColor())
                .frame(width: 24, height: 24)
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.tertiary.opacity(0.3))
                        .frame(width: 52, height: 52)
                }
        }
        .buttonStyle(.plain)
    }

    private var colorPickerButton: some View {
        Button {
            shouldShowColorPicker = true
        } label: {
            YabaIconView(bundleKey: "paint-board")
                .aspectRatio(contentMode: .fit)
                .foregroundStyle(machine.state.colorRole.getUIColor())
                .frame(width: 24, height: 24)
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.tertiary.opacity(0.3))
                        .frame(width: 52, height: 52)
                }
        }
        .buttonStyle(.plain)
    }

    private var labelField: some View {
        TextField(
            "",
            text: Binding(
                get: { machine.state.label },
                set: { new in
                    Task {
                        await machine.send(.onChangeLabel(new))
                    }
                }
            ),
            prompt: Text("Create Folder Placeholder")
        )
        .padding(.horizontal)
        .frame(height: 52)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tertiary.opacity(0.3))
        }
    }

    private var descriptionField: some View {
        TextField(
            "",
            text: Binding(
                get: { machine.state.folderDescription },
                set: { new in
                    Task {
                        await machine.send(.onChangeDescription(new))
                    }
                }
            ),
            prompt: Text("Create Bookmark Description Placeholder"),
            axis: .vertical
        )
        .lineLimit(3...6)
        .padding(.horizontal)
        .padding(.vertical, 8)
        .frame(minHeight: 72, alignment: .topLeading)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tertiary.opacity(0.3))
        }
    }

    private var parentFolderRow: some View {
        PresentableFolderItemView(
            model: parentFolderModel,
            nullModelPresentableColor: machine.state.colorRole,
            onPressed: {
                shouldShowParentPicker = true
            }
        )
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tertiary.opacity(0.3))
        }
    }

    private var parentFolderModel: FolderModel? {
        guard let id = machine.state.parentFolderId else { return nil }
        let predicate = #Predicate<FolderModel> { $0.folderId == id }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        return try? modelContext.fetch(descriptor).first
    }

    private func hydrateIfNeeded() async {
        guard let existingFolderId else { return }
        let predicate = #Predicate<FolderModel> { $0.folderId == existingFolderId }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        guard let folder = try? modelContext.fetch(descriptor).first else { return }

        await machine.send(.onInitWithFolder(folderId: existingFolderId))
        await machine.send(.onChangeLabel(folder.label))
        await machine.send(.onChangeDescription(folder.folderDescription ?? ""))
        await machine.send(.onSelectNewIcon(folder.icon))
        await machine.send(.onSelectNewColor(YabaColor.from(colorCode: folder.colorRaw)))
        await machine.send(.onSelectNewParent(parentFolderId: folder.parent?.folderId))
    }
}
