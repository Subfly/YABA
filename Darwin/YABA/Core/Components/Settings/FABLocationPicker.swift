//
//  FABLocationPicker.swift
//  YABA
//
//  Created by Ali Taha on 11.09.2025.
//

import SwiftUI

struct FABLocationPicker: View {
    @AppStorage(Constants.preferredFabPositionKey)
    private var preferredPosition: FABPosition = .center
    
    var body: some View {
        Menu {
            ForEach(FABPosition.allCases, id: \.self) { position in
                Button {
                    withAnimation {
                        preferredPosition = position
                    }
                } label: {
                    Label {
                        HStack {
                            if position == preferredPosition {
                                YabaIconView(bundleKey: "tick-01")
                            }
                            Text(position.getUITitle())
                        }
                    } icon: {
                        YabaIconView(bundleKey: position.getUIIconName())
                    }
                }
            }
        } label: {
            HStack {
                Label {
                    Text("Settings FAB Position Title")
                } icon: {
                    YabaIconView(bundleKey: "add-circle")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                HStack {
                    YabaIconView(bundleKey: preferredPosition.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredPosition.getUITitle())
                }
            }.contentShape(Rectangle())
        }.buttonStyle(.plain)
    }
}

#Preview {
    FABLocationPicker()
}
