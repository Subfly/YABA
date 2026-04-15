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

    @State
    private var shouldShowEditSheet = false
    @State
    private var shouldShowDeleteAlert = false

    var body: some View {
        BaseAnnotationItemView(annotation: annotation, interactive: true, onTap: onPress)
            #if !KEYBOARD_EXTENSION
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button {
                    shouldShowDeleteAlert = true
                } label: {
                    Text("TODO")
                }
                .tint(.red)
                Button {
                    shouldShowEditSheet = true
                } label: {
                    Text("TODO")
                }
                .tint(.orange)
            }
            #endif
            .sheet(isPresented: $shouldShowEditSheet) {
                // TODO: Annotation editor
                EmptyView()
            }
            .alert("TODO", isPresented: $shouldShowDeleteAlert) {
                Button("TODO", role: .cancel) {
                    shouldShowDeleteAlert = false
                }
                Button("TODO", role: .destructive) {
                    shouldShowDeleteAlert = false
                }
            } message: {
                Text("TODO")
            }
    }
}
