//
//  PrivateBookmarkSheetSectionHeader.swift
//  YABA
//
//  Section headers for PIN password sheets (List sections).
//

import SwiftUI

struct PrivateBookmarkSheetSectionHeader: View {
    let titleKey: LocalizedStringKey
    let icon: String

    var body: some View {
        Label {
            Text(titleKey)
        } icon: {
            YabaIconView(bundleKey: icon)
                .frame(width: 22, height: 22)
        }
    }
}
