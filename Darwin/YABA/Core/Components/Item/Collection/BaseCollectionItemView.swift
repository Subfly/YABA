//
//  BaseCollectionItemView.swift
//  YABA
//
//  Shared layout: optional hierarchy color bars + row content (Compose parity).
//

import SwiftUI

struct BaseCollectionItemView<Content: View>: View {
    let parentColors: [YabaColor]
    @ViewBuilder var content: () -> Content

    var body: some View {
        HStack(alignment: .center, spacing: 4) {
            if !parentColors.isEmpty {
                HierarchyColorBarsView(parentColors: parentColors)
            }
            content()
        }
    }
}
