//
//  TagCreationContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI

struct TagCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Environment(\.modelContext)
    private var modelContext

    @State
    private var machine = TagCreationStateMachine(
        initialState: TagCreationUIState(colorRole: .blue)
    )

    @State
    private var shouldShowIconPicker = false
    @State
    private var shouldShowColorPicker = false

    private let existingTagId: String?

    init(existingTagId: String? = nil) {
        self.existingTagId = existingTagId
    }

    private var navigationTitleKey: String {
        existingTagId == nil ? "Create Tag Title" : "Edit Tag Title"
    }

    private var canSave: Bool {
        !machine.state.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        NavigationStack {
            HStack(alignment: .center, spacing: 24) {
                iconPickerButton
                labelField
                colorPickerButton
            }
            .padding(.all)
            .padding(.horizontal)
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
        }
        .presentationDetents([.fraction(0.2)])
        .presentationDragIndicator(.visible)
        .task(id: existingTagId) {
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
            prompt: Text("Create Tag Placeholder")
        )
        .padding(.horizontal)
        .frame(height: 52)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tertiary.opacity(0.3))
        }
    }

    private func hydrateIfNeeded() async {
        guard let existingTagId else { return }
        let predicate = #Predicate<TagModel> { $0.tagId == existingTagId }
        var descriptor = FetchDescriptor(predicate: predicate)
        descriptor.fetchLimit = 1
        guard let tag = try? modelContext.fetch(descriptor).first else { return }

        await machine.send(.onInitWithTag(tagId: existingTagId))
        await machine.send(.onChangeLabel(tag.label))
        await machine.send(.onSelectNewIcon(tag.icon))
        await machine.send(.onSelectNewColor(YabaColor.from(colorCode: tag.colorRaw)))
    }
}
