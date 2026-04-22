//
//  AnnotationCreationSheet.swift
//  YABA
//

import SwiftUI

enum AnnotationCreationSheetMode: Identifiable {
    case create(ReadableSelectionDraft)
    case edit(AnnotationModel)

    var id: String {
        switch self {
        case let .create(draft):
            return "create:\(draft.bookmarkId):\(draft.readableVersionId)"
        case let .edit(annotation):
            return "edit:\(annotation.annotationId)"
        }
    }
}

enum AnnotationCreationSheetOutcome {
    case cancelled
    case persisted
    case readableCreateRequested(annotationId: String, request: AnnotationReadableCreateRequest)
    case readableDeleteRequested(annotationId: String, readableVersionId: String)
}

struct AnnotationCreationSheet: View {
    let mode: AnnotationCreationSheetMode
    let onFinish: (AnnotationCreationSheetOutcome) -> Void

    @Environment(\.dismiss)
    private var dismiss

    @State
    private var machine: AnnotationCreationStateMachine

    @State
    private var shouldShowColorPicker = false

    @State
    private var shouldShowDeleteConfirmation = false

    init(mode: AnnotationCreationSheetMode, onFinish: @escaping (AnnotationCreationSheetOutcome) -> Void) {
        self.mode = mode
        self.onFinish = onFinish
        _machine = State(initialValue: AnnotationCreationStateMachine(initialState: Self.initialState(for: mode)))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Annotation Quote Section Title") {
                    Text(machine.state.quoteText?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                        ?? String(localized: "Annotation Quote Empty Message"))
                        .foregroundStyle(
                            machine.state.quoteText?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty == nil
                                ? .secondary
                                : .primary
                        )
                }

                Section("Annotation Note Section Title") {
                    TextField(
                        "",
                        text: Binding(
                            get: { machine.state.note },
                            set: { newValue in
                                Task { await machine.send(.onChangeNote(newValue)) }
                            }
                        ),
                        prompt: Text("Annotation Note Placeholder")
                    )
                }

                Section("Annotation Color Section Title") {
                    Button {
                        shouldShowColorPicker = true
                    } label: {
                        HStack(spacing: 10) {
                            Circle()
                                .fill(machine.state.colorRole.getUIColor())
                                .frame(width: 18, height: 18)
                            Text(machine.state.colorRole.getUIText())
                            Spacer(minLength: 0)
                            YabaIconView(bundleKey: "arrow-right-01")
                                .frame(width: 16, height: 16)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                if isEditing {
                    Section {
                        Button(role: .destructive) {
                            shouldShowDeleteConfirmation = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle(
                Text(
                    isEditing
                        ? LocalizedStringKey("Annotation Edit Title")
                        : LocalizedStringKey("Annotation Creation Title")
                )
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onFinish(.cancelled)
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        Task {
                            await handleSave()
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .sheet(isPresented: $shouldShowColorPicker) {
                YabaColorPicker(
                    selection: Binding(
                        get: { machine.state.colorRole },
                        set: { color in
                            Task { await machine.send(.onSelectNewColor(color)) }
                        }
                    ),
                    onDismiss: { shouldShowColorPicker = false }
                )
            }
            .alert(
                "Annotation Delete Confirmation Title",
                isPresented: $shouldShowDeleteConfirmation
            ) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    Task {
                        await handleDelete()
                    }
                }
            } message: {
                Text("Annotation Delete Confirmation Message")
            }
        }
        .presentationDetents([.medium, .large])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
    }

    private var isEditing: Bool {
        if case .edit = mode { return true }
        return false
    }

    private var canSave: Bool {
        if isEditing { return true }
        return machine.state.quoteText?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false
    }

    private func handleSave() async {
        switch mode {
        case let .create(originalDraft):
            if machine.state.annotationType == .readable {
                let annotationId = UUID().uuidString
                let request = AnnotationReadableCreateRequest(
                    selectionDraft: ReadableSelectionDraft(
                        bookmarkId: machine.state.bookmarkId,
                        readableVersionId: machine.state.readableVersionId ?? originalDraft.readableVersionId,
                        quoteText: machine.state.quoteText,
                        extrasJson: machine.state.extrasJson,
                        annotationType: machine.state.annotationType
                    ),
                    colorRole: machine.state.colorRole,
                    note: machine.state.note.nilIfEmpty
                )
                onFinish(.readableCreateRequested(annotationId: annotationId, request: request))
            } else {
                await machine.send(.onSave)
                onFinish(.persisted)
            }
        case .edit:
            await machine.send(.onSave)
            onFinish(.persisted)
        }
        dismiss()
    }

    private func handleDelete() async {
        switch mode {
        case .create:
            onFinish(.cancelled)
            dismiss()
        case let .edit(annotation):
            if annotation.type == .readable {
                let versionId = annotation.readableVersion?.readableVersionId ?? machine.state.readableVersionId ?? ""
                guard !versionId.isEmpty else {
                    await machine.send(.onDelete)
                    onFinish(.persisted)
                    dismiss()
                    return
                }
                onFinish(
                    .readableDeleteRequested(
                        annotationId: annotation.annotationId,
                        readableVersionId: versionId
                    )
                )
            } else {
                await machine.send(.onDelete)
                onFinish(.persisted)
            }
            dismiss()
        }
    }

    private static func initialState(for mode: AnnotationCreationSheetMode) -> AnnotationCreationUIState {
        switch mode {
        case let .create(draft):
            return AnnotationCreationUIState(
                bookmarkId: draft.bookmarkId,
                readableVersionId: draft.readableVersionId,
                annotationId: nil,
                annotationType: draft.annotationType,
                colorRole: .none,
                note: "",
                quoteText: draft.quoteText,
                extrasJson: draft.extrasJson,
                lastError: nil
            )
        case let .edit(annotation):
            return AnnotationCreationUIState(
                bookmarkId: annotation.bookmark?.bookmarkId ?? "",
                readableVersionId: annotation.readableVersion?.readableVersionId,
                annotationId: annotation.annotationId,
                annotationType: annotation.type,
                colorRole: annotation.colorRole,
                note: annotation.note ?? "",
                quoteText: annotation.quoteText,
                extrasJson: annotation.extrasJson,
                lastError: nil
            )
        }
    }
}
