//
//  ConentAppearancePicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

struct ContentAppearancePicker: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var preferredContentAppearance: ViewType = .list
    
    var body: some View {
        Menu {
            ForEach(ViewType.allCases, id: \.self) { type in
                Button {
                    withAnimation {
                        preferredContentAppearance = type
                    }
                } label: {
                    Label {
                        HStack {
                            if type == preferredContentAppearance {
                                YabaIconView(bundleKey: "tick-01")
                            }
                            Text(type.getUITitle())
                        }
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            }
        } label: {
            HStack {
                Label {
                    Text("Settings Content Appearance Title")
                } icon: {
                    YabaIconView(bundleKey: "change-screen-mode")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                HStack {
                    YabaIconView(bundleKey: preferredContentAppearance.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredContentAppearance.getUITitle())
                }
            }
        }.buttonStyle(.plain)
    }
}

#Preview {
    ContentAppearancePicker()
}
