//
//  ItemListRowChrome.swift
//  YABA
//
//  `List` row backgrounds: selection + optional hover (Catalyst / iPad); iPhone uses clear for system styling.
//

import SwiftUI
import UIKit

enum ItemListRowChrome {
    /// Row background for folder, tag, and bookmark rows inside a `List`.
    @ViewBuilder
    static func listRowBackground(
        cornerRadius: CGFloat,
        isSelected: Bool,
        isHovered: Bool
    ) -> some View {
        #if targetEnvironment(macCatalyst)
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(rowFill(isSelected: isSelected, isHovered: isHovered))
        #else
        if UIDevice.current.userInterfaceIdiom == .phone {
            Color.clear
        } else {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(rowFill(isSelected: isSelected, isHovered: isHovered))
        }
        #endif
    }

    private static func rowFill(isSelected: Bool, isHovered: Bool) -> Color {
        if isSelected {
            Color.gray.opacity(0.2)
        } else if isHovered {
            Color.gray.opacity(0.1)
        } else {
            Color.clear
        }
    }
}
