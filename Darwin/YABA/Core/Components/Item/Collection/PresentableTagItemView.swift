//
//  PresentableTagItemView.swift
//  YABA
//
//  Picker / sheet row (nullable tag). Compose `PresentableTagItemView` parity.
//

import SwiftUI
import YABACore

struct PresentableTagItemView: View {
    let model: TagModel?
    let nullModelPresentableColor: YabaColor
    let onPressed: () -> Void
    let onNavigateToEdit: () -> Void

    @State
    private var itemState = CollectionItemState()

    var body: some View {
        Button(action: onPressed) {
            HStack {
                YabaIconView(bundleKey: model?.icon ?? "tag-01")
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
                Text(model?.label ?? "TODO")
                Spacer()
            }
        }
        .buttonStyle(.plain)
        .onHover { itemState.isHovered = $0 }
        .contextMenu {
            Button("TODO") {
                onNavigateToEdit()
            }
        }
        .sheet(isPresented: $itemState.shouldShowEditSheet) {
            // TODO: Tag edit from presenter
            EmptyView()
        }
    }
}
