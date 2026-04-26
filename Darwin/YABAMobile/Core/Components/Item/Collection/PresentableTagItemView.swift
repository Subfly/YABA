//
//  PresentableTagItemView.swift
//  YABA
//
//  Picker / sheet row (nullable tag). Compose `PresentableTagItemView` parity.
//

import SwiftUI

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
                    .frame(width: 24, height: 24)
                    .foregroundStyle((model?.color ?? nullModelPresentableColor).getUIColor())
                if let model {
                    if model.tagId == Constants.Tag.Pinned.id {
                        Text(LocalizedStringKey(Constants.Tag.Pinned.name))
                    } else {
                        Text(model.label)
                    }
                } else {
                    Text(LocalizedStringKey("Select Tags No Tags Selected Title"))
                }
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
            if let model {
                TagCreationContent(existingTagId: model.tagId)
            }
        }
    }
}
