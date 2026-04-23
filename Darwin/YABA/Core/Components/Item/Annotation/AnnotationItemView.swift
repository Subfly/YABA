//
//  AnnotationItemView.swift
//  YABA
//
//  Interactive annotation row: swipe + menus can be added by parent later; sheets stubbed with TODO.
//

import SwiftUI

struct AnnotationItemView: View {
    let annotation: AnnotationModel
    let onPress: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void

    @State
    private var shouldShowDeleteAlert = false

    var body: some View {
        BaseAnnotationItemView(annotation: annotation, interactive: true, onTap: onPress)
            #if !KEYBOARD_EXTENSION
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button {
                    shouldShowDeleteAlert = true
                } label: {
                    swipeLabel(iconKey: "delete-02", titleKey: "Delete")
                }
                .tint(YabaColor.red.getUIColor())
                Button {
                    onEdit()
                } label: {
                    swipeLabel(iconKey: "edit-02", titleKey: "Edit")
                }
                .tint(YabaColor.orange.getUIColor())
            }
            #endif
            .alert("Annotation Delete Confirmation Title", isPresented: $shouldShowDeleteAlert) {
                Button("Cancel", role: .cancel) {
                    shouldShowDeleteAlert = false
                }
                Button("Delete", role: .destructive) {
                    onDelete()
                    shouldShowDeleteAlert = false
                }
            } message: {
                Text("Annotation Delete Confirmation Message")
            }
    }
    
    @ViewBuilder
    private func swipeLabel(iconKey: String, titleKey: String) -> some View {
        VStack(spacing: 2) {
            YabaIconView(bundleKey: iconKey)
                .scaledToFit()
                .frame(width: 22, height: 22)
            Text(LocalizedStringKey(titleKey))
                .font(.caption2)
        }
    }
}
