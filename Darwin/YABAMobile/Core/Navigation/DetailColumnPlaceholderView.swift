//
//  DetailColumnPlaceholderView.swift
//  YABA
//
//  Empty detail column — copy from legacy `CollectionDetail` (no selection).
//

import SwiftUI

struct DetailColumnPlaceholderView: View {
    var body: some View {
        ContentUnavailableView {
            Label {
                Text("No Selected Collection Title")
            } icon: {
                YabaIconView(bundleKey: "dashboard-square-02")
                    .scaledToFit()
                    .frame(width: 52, height: 52)
            }
        } description: {
            Text("No Selected Collection Message")
        }
    }
}
