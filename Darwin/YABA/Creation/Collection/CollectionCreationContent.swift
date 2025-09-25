//
//  CreateTagContent.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI
import WidgetKit

struct CollectionCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: CollectionCreationState = .init()
    
    private let navigationTitle: String
    private let textFieldPlaceholder: String
    
    let collectionToAdd: YabaCollection?
    let collectionToEdit: YabaCollection?
    let collectionType: CollectionType
    let onEditCallback: (YabaCollection) -> Void
    
    init(
        collectionType: CollectionType, // Used when creating a collection
        collectionToAdd: YabaCollection?, // Used when adding a collection to collection
        collectionToEdit: YabaCollection?, // Used when editing a collection
        onEditCallback: @escaping (YabaCollection) -> Void
    ) {
        self.collectionToAdd = collectionToAdd
        self.collectionToEdit = collectionToEdit
        self.collectionType = collectionType
        self.onEditCallback = onEditCallback
        
        if let content = collectionToEdit {
            self.navigationTitle = switch content.collectionType {
                case .folder: "Edit Folder Title"
                case .tag: "Edit Tag Title"
            }
            self.textFieldPlaceholder = switch content.collectionType {
                case .folder: "Create Folder Placeholder"
                case .tag: "Create Tag Placeholder"
            }
        } else {
            self.navigationTitle = switch collectionType {
                case .folder: "Create Folder Title"
                case .tag: "Create Tag Title"
            }
            self.textFieldPlaceholder = switch collectionType {
                case .folder: "Create Folder Placeholder"
                case .tag: "Create Tag Placeholder"
            }
        }
    }
    
    var body: some View {
        NavigationView {
            VStack {
                HStack(alignment: .center, spacing: 24) {
                    generateIconPickerButton(
                        frameSize: 24,
                        backgroundSize: 52
                    )
                    textField
                        .background {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(.tertiary.opacity(0.3))
                                .frame(height: 52)
                        }
                    generateColorPickerButton(
                        frameSize: 24,
                        backgroundSize: 52
                    )
                }
                .padding()
                .padding(.horizontal)
                
                NavigationLink {
                    SelectFolderContent(
                        mode: .parent,
                        folderInAction: nil, // Shows all the other folders
                        selectedFolder: collectionToAdd,
                        onSelectNewFolder: { newFolder in
                            state.selectedParent = newFolder
                        },
                        onEditSelectedFolderDuringCreation: { editedFolder in
                            state.selectedParent = editedFolder
                        },
                        onDeleteSelectedFolderDuringCreation: {
                            state.selectedParent = nil
                        }
                    )
                } label: {
                    HStack {
                        Text("Parent Folder Label")
                        Spacer()
                        HStack {
                            if let selected = state.selectedParent {
                                YabaIconView(bundleKey: selected.icon)
                                    .frame(width: 24, height: 24)
                                    .foregroundStyle(selected.color.getUIColor())
                                Text(selected.label)
                            } else {
                                Text("Root Parent Label")
                            }
                        }
                    }
                    .padding(.horizontal)
                    .background {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.tertiary.opacity(0.3))
                            .frame(height: 52)
                    }
                    .contentShape(Rectangle())
                    .padding()
                }.buttonStyle(.plain)
            }
            .sheet(isPresented: $state.shouldShowIconPicker) {
                iconPicker
            }
            .sheet(isPresented: $state.shouldShowColorPicker) {
                colorPicker
            }
            .navigationTitle(LocalizedStringKey(navigationTitle))
            .toolbarTitleDisplayMode(.inline)
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
                        onDone()
                    } label: {
                        Text("Done")
                    }
                    .disabled(state.collectionName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
        #if !targetEnvironment(macCatalyst)
        .presentationDragIndicator(.visible)
        #endif
        .onAppear(perform: onAppear)
    }
    
    @ViewBuilder
    private func generateIconPickerButton(
        frameSize: CGFloat,
        backgroundSize: CGFloat,
    ) -> some View {
        YabaIconView(bundleKey: state.selectedIconName)
            .aspectRatio(contentMode: .fit)
            .foregroundStyle(state.selectedColor.getUIColor())
            .frame(width: frameSize, height: frameSize)
            .background {
                RoundedRectangle(cornerRadius: 12)
                    .fill(.tertiary.opacity(0.3))
                    .frame(width: backgroundSize, height: backgroundSize)
            }
            .onTapGesture {
                state.shouldShowIconPicker = true
            }
    }
    
    @ViewBuilder
    private func generateColorPickerButton(
        frameSize: CGFloat,
        backgroundSize: CGFloat,
    ) -> some View {
        YabaIconView(bundleKey: "paint-board")
            .aspectRatio(contentMode: .fit)
            .foregroundStyle(state.selectedColor.getUIColor())
            .frame(width: frameSize, height: frameSize)
            .background {
                RoundedRectangle(cornerRadius: 12)
                    .fill(.tertiary.opacity(0.3))
                    .frame(width: backgroundSize, height: backgroundSize)
            }
            .onTapGesture {
                state.shouldShowColorPicker = true
            }
    }
    
    @ViewBuilder
    private var textField: some View {
        TextField(
            text: $state.collectionName,
            prompt: Text(LocalizedStringKey(textFieldPlaceholder))
        ) {
            Text("")
        }
        .padding(.horizontal)
    }
    
    @ViewBuilder
    private var iconPicker: some View {
        IconPickerView(
            onSelectIcon: { newName in
                withAnimation {
                    state.selectedIconName = newName
                    state.shouldShowIconPicker = false
                }
            },
            onCancel: {
                state.shouldShowIconPicker = false
            }
        )
    }
    
    @ViewBuilder
    private var colorPicker: some View {
        YabaColorPicker(
            selection: $state.selectedColor,
            onDismiss: {
                state.shouldShowColorPicker = false
            }
        )
    }
    
    func onAppear() {
        switch collectionType {
        case .folder:
            state.selectedIconName = "folder-01"
        case .tag:
            state.selectedIconName = "tag-01"
        }
        
        if let collectionToEdit {
            state.collectionName = collectionToEdit.label
            state.selectedIconName = collectionToEdit.icon
            state.selectedColor = collectionToEdit.color
            
            if let collectionToAdd {
                state.selectedParent = collectionToAdd
            } else if collectionToEdit.parentCollection != nil {
                state.selectedParent = collectionToEdit.parentCollection
            }
        }
    }
    
    func onDone() {
        withAnimation {
            if let collectionToEdit {
                collectionToEdit.label = state.collectionName
                collectionToEdit.icon = state.selectedIconName
                collectionToEdit.color = state.selectedColor
                collectionToEdit.parentCollection = state.selectedParent
                collectionToEdit.editedAt = .now
                collectionToEdit.version += 1
                
                onEditCallback(collectionToEdit)
            } else {
                let collection = YabaCollection(
                    collectionId: UUID().uuidString,
                    label: state.collectionName,
                    icon: state.selectedIconName,
                    createdAt: .now,
                    editedAt: .now,
                    parentCollection: state.selectedParent,
                    color: state.selectedColor,
                    type: collectionType,
                    version: 0,
                )
                
                modelContext.insert(collection)
            }
            
            try? modelContext.save()
            
            WidgetCenter.shared.reloadAllTimelines()
            
            dismiss()
        }
    }
}

#Preview {
    CollectionCreationContent(
        collectionType: .folder,
        collectionToAdd: .empty(),
        collectionToEdit: .empty(),
        onEditCallback: { _ in }
    )
}
