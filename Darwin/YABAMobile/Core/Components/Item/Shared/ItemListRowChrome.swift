//
//  ItemListRowChrome.swift
//  YABA
//
//  `List` row backgrounds: hover highlight on Mac Catalyst only; elsewhere the system list shows through.
//

import SwiftUI

enum ItemListRowChrome {
    /// Row background for folder, tag, and bookmark rows inside a `List`.
    @ViewBuilder
    static func listRowBackground(
        cornerRadius: CGFloat,
        isHovered: Bool
    ) -> some View {
        #if targetEnvironment(macCatalyst)
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(isHovered ? Color.gray.opacity(0.1) : Color.clear)
        #else
        Color.clear
        #endif
    }
}
