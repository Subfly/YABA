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
                    Text("Delete")
                }
                .tint(.red)
                Button {
                    onEdit()
                } label: {
                    Text("Edit")
                }
                .tint(.orange)
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
}
