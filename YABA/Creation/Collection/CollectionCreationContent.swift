//
//  CreateTagContent.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

struct CollectionCreationContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: CollectionCreationState = .init()
    
    private let navigationTitle: String
    private let textFieldPlaceholder: String
    
    @Binding
    var collectionToEdit: YabaCollection?
    let collectionType: CollectionType
    let onEditCallback: (YabaCollection) -> Void
    
    init(
        collectionType: CollectionType,
        collectionToEdit: Binding<YabaCollection?>,
        onEditCallback: @escaping (YabaCollection) -> Void
    ) {
        _collectionToEdit = collectionToEdit
        self.collectionType = collectionType
        self.onEditCallback = onEditCallback
        
        if let content = collectionToEdit.wrappedValue {
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
        .presentationDetents([.fraction(0.25)])
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
        Image(systemName: state.selectedIconName)
            .resizable()
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
        Image(systemName: "swatchpalette")
            .resizable()
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
            Text("Create Collection")
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
            state.selectedIconName = "folder"
        case .tag:
            state.selectedIconName = "tag"
        }
        
        if let collectionToEdit {
            state.collectionName = collectionToEdit.label
            state.selectedIconName = collectionToEdit.icon
            state.selectedColor = collectionToEdit.color
        }
    }
    
    func onDone() {
        withAnimation {
            if let collectionToEdit {
                collectionToEdit.label = state.collectionName
                collectionToEdit.icon = state.selectedIconName
                collectionToEdit.color = state.selectedColor
                onEditCallback(collectionToEdit)
            } else {
                let collection = YabaCollection(
                    label: state.collectionName,
                    icon: state.selectedIconName,
                    createdAt: .now,
                    color: state.selectedColor,
                    type: collectionType
                )
                modelContext.insert(collection)
            }
            try? modelContext.save()
            dismiss()
        }
    }
}

#Preview {
    CollectionCreationContent(
        collectionType: .folder,
        collectionToEdit: .constant(.empty()),
        onEditCallback: { _ in }
    )
}
