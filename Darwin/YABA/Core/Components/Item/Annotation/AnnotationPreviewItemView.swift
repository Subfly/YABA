//
//  AnnotationPreviewItemView.swift
//  YABA
//
//  Read-only annotation row (no swipe / tap actions).
//

import SwiftUI

struct AnnotationPreviewItemView: View {
    let annotation: AnnotationModel

    var body: some View {
        BaseAnnotationItemView(annotation: annotation, interactive: false, onTap: {})
            .allowsHitTesting(false)
    }
}
