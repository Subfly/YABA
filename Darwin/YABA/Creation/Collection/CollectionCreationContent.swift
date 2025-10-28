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
    private let presentationDetentsFraction: CGFloat
    
    let collectionToEdit: YabaCollection?
    let collectionType: CollectionType
    let onEditCallback: (YabaCollection) -> Void
    
    init(
        collectionType: CollectionType, // Used when creating a collection
        collectionToEdit: YabaCollection?, // Used when editing a collection
        onEditCallback: @escaping (YabaCollection) -> Void
    ) {
        self.collectionToEdit = collectionToEdit
        self.collectionType = collectionType
        self.onEditCallback = onEditCallback
        
        self.presentationDetentsFraction = collectionType == .folder ? 0.3 : 0.25
        
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
            VStack(spacing: 24) {
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
                .padding(.horizontal)
                if collectionType == .folder {
                    parentFolderPicker
                }
            }
            .padding()
            .sheet(isPresented: $state.shouldShowIconPicker) {
                iconPicker
            }
            .sheet(isPresented: $state.shouldShowColorPicker) {
                colorPicker
            }
            .sheet(isPresented: $state.shouldShowParentFolderPicker) {
                NavigationView {
                    SelectFolderContent(
                        selectedFolder: $state.selectedParentFolder,
                        mode: .parentSelection(collectionToEdit) { moveToRoot in
                            // TODO: INDICATE FOLDER WILL BE VISIBLE IN THE ROOT
                            state.moveToRoot = moveToRoot
                            state.shouldShowParentFolderPicker = false
                        }
                    )
                }
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
        .presentationDetents([.fraction(presentationDetentsFraction)])
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
    
    @ViewBuilder
    private var parentFolderPicker: some View {
        HStack {
            if let parent = state.selectedParentFolder {
                YabaIconView(bundleKey: parent.icon)
                    .frame(width: 24, height: 24)
                    .foregroundStyle(parent.color.getUIColor())
                Text(parent.label)
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            } else {
                Text("Folder Creation Select Folder Message")
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding()
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tertiary.opacity(0.3))
        }
        .onTapGesture {
            state.shouldShowParentFolderPicker = true
        }
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
            state.selectedParentFolder = collectionToEdit.parent
        }
    }
    
    func onDone() {
        withAnimation {
            if let collectionToEdit {
                collectionToEdit.label = state.collectionName
                collectionToEdit.icon = state.selectedIconName
                collectionToEdit.color = state.selectedColor
                collectionToEdit.editedAt = .now
                collectionToEdit.version += 1
                
                if collectionToEdit.parent != state.selectedParentFolder {
                    collectionToEdit.parent = state.selectedParentFolder
                    state.selectedParentFolder?.version += 1
                }
                
                onEditCallback(collectionToEdit)
            } else {
                let collection = YabaCollection(
                    collectionId: UUID().uuidString,
                    label: state.collectionName,
                    icon: state.selectedIconName,
                    createdAt: .now,
                    editedAt: .now,
                    color: state.selectedColor,
                    type: collectionType,
                    version: 0,
                    parent: state.selectedParentFolder
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
        collectionToEdit: .empty(),
        onEditCallback: { _ in }
    )
}
