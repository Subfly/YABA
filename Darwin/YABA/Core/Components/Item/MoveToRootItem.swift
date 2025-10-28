//
//  MoveToRootItem.swift
//  YABA
//
//  Created by Ali Taha on 28.10.2025.
//

import SwiftUI

struct MoveToRootItem: View {
    @State
    private var isHovered: Bool = false
    
    var body: some View {
        HStack {
            Label {
                Text("Select Folder Move To Root Label")
            } icon: {
                YabaIconView(bundleKey: "arrow-move-up-right")
            }
            Spacer()
        }
        .contentShape(Rectangle())
        #if targetEnvironment(macCatalyst)
        .listRowBackground(
            isHovered
            ? RoundedRectangle(cornerRadius: 8).fill(Color.gray.opacity(0.1))
            : RoundedRectangle(cornerRadius: 8).fill(Color.clear)
        )
        #endif
        .onHover { hovered in
            isHovered = hovered
        }
    }
}

#Preview {
    MoveToRootItem()
}
