//
//  ThemePicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

struct ThemePicker: View {
    @AppStorage(Constants.preferredThemeKey)
    private var preferredTheme: ThemeType = .system
    
    var body: some View {
        Menu {
            ForEach(ThemeType.allCases, id: \.self) { type in
                Button {
                    withAnimation {
                        preferredTheme = type
                    }
                } label: {
                    Label {
                        HStack {
                            if type == preferredTheme {
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
                    Text("Settings Theme Title")
                } icon: {
                    YabaIconView(bundleKey: "paint-brush-02")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                HStack {
                    YabaIconView(bundleKey: preferredTheme.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredTheme.getUITitle())
                }
            }.contentShape(Rectangle())
        }.buttonStyle(.plain)
    }
}

#Preview {
    ThemePicker()
}
