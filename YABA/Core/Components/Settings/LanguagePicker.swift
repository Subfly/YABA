//
//  LanguagePicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

struct LanguagePicker: View {
    var body: some View {
        HStack {
            Label {
                Text("Settings Language Title")
            } icon: {
                YabaIconView(bundleKey: "earth")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
            }
            Spacer()
            Label {
                Text(Bundle.main.preferredLocalizations[0].localizedUppercase)
            } icon: {
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }.labelStyle(InverseLabelStyle())
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if let url: URL = .init(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
    }
}

#Preview {
    LanguagePicker()
}
