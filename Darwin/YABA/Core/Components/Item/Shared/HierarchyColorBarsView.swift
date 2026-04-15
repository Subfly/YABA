//
//  HierarchyColorBarsView.swift
//  YABA
//
//  Parent folder color bars (Compose parity: bars outside swipe region).
//

import SwiftUI
import YABACore

/// Vertical color strips for ancestor folders, left of the row content.
struct HierarchyColorBarsView: View {
    let parentColors: [YabaColor]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(Array(parentColors.enumerated()), id: \.offset) { _, color in
                color
                    .getUIColor()
                    .frame(width: 4, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: 2))
            }
        }
    }
}
