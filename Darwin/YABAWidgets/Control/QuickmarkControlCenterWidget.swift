//
//  QuickmarkControlCenterWidget.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 26.08.2025.
//

import SwiftUI
import WidgetKit

struct QuickmarkControlCenterWidget: ControlWidget {
    private let kind = "YABA Quickmark"
    
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: kind) {
            ControlWidgetButton(action: QuickmarkIntent()) {
                // Thanks Apple for not letting me use it normally
                // And when I use any converter, it just fucks up like this
                Label("Quick Bookmark", image: .quickMarkIcon)
            }
        }
        .displayName("YABA Quickmark")
        .description("Widget Create a Quick Bookmark")
    }
}
