//
//  CollectionItemOverflowMenu.swift
//  YABA
//
//  Catalyst: optional overflow; other platforms always show menu when used.
//

import SwiftUI

struct CollectionItemOverflowMenu<MenuContent: View>: View {
    @Binding
    var isHovered: Bool
    let menuContent: () -> MenuContent

    private var isCatalystEnv: Bool {
        #if targetEnvironment(macCatalyst)
        true
        #else
        false
        #endif
    }

    var body: some View {
        if isCatalystEnv {
            if isHovered {
                Menu {
                    menuContent()
                } label: {
                    YabaIconView(bundleKey: "more-horizontal-circle-02")
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
            }
        } else {
            Menu {
                menuContent()
            } label: {
                YabaIconView(bundleKey: "more-horizontal-circle-02")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
            }
        }
    }
}
