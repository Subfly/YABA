//
//  YABALauncherControlCenterWidget.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 26.08.2025.
//

import SwiftUI
import WidgetKit
import AppIntents

struct YABALauncherControlCenterWidget: ControlWidget {
    private let kind = "YABA Opener"
    
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: kind) {
            ControlWidgetButton(action: OpenYABAIntent()) {
                // Thanks Apple for not letting me use it normally
                // And when I use any converter, it just fucks up like this
                Label("Open YABA", image: .fakeAppIcon)
            }
        }
        .displayName("YABA")
        .description("Open YABA")
    }
}
