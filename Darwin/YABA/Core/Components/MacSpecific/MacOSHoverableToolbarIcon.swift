//
//  MacOSHoverableToolbarIcon.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//

import SwiftUI

struct MacOSHoverableToolbarIcon: View {
    @State
    private var isHovered: Bool = false
    
    let bundleKey: String
    let tooltipKey: String
    let onPressed: () -> Void
    
    var body: some View {
        Button {
            onPressed()
        } label: {
            YabaIconView(bundleKey: bundleKey)
                .scaledToFit()
                .frame(width: 28, height: 28)
                .padding(8)
                .background {
                    if isHovered {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(.gray.opacity(0.1))
                    }
                }
        }
        .onHover { hovered in
            isHovered = hovered
        }
        .help(LocalizedStringKey(tooltipKey))
    }
}
