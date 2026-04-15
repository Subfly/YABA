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
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
                Text(model?.label ?? "TODO")
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
            }
        }
        .buttonStyle(.plain)
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            // TODO: Edit selected folder from presenter
            EmptyView()
        }
    }
}
