//
//  PresentableFolderItemView.swift
//  YABA
//
//  Picker / sheet row (nullable folder). Compose `PresentableFolderItemView` parity.
//

import SwiftUI

struct PresentableFolderItemView: View {
    let model: FolderModel?
    let nullModelPresentableColor: YabaColor
    let onPressed: () -> Void

    @State
    private var itemState = CollectionItemState()

    var body: some View {
        Button(action: onPressed) {
            HStack {
                YabaIconView(bundleKey: model?.icon ?? "folder-01")
                    .frame(width: 24, height: 24)
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
                if let model {
                    if model.folderId == Constants.uncategorizedCollectionId {
                        Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                    } else {
                        Text(model.label)
                    }
                } else {
                    Text(LocalizedStringKey("Folder Creation Select Folder Message"))
                }
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .frame(width: 22, height: 22)
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
            }
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            if let model {
                FolderCreationContent(existingFolderId: model.folderId)
            }
        }
    }
}
