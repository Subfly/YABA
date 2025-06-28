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
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var preferredCardViewImageSizing: CardViewTypeImageSizing = .small
    
    var body: some View {
        Menu {
            Button {
                withAnimation {
                    preferredContentAppearance = .list
                }
            } label: {
                Label {
                    HStack {
                        if preferredContentAppearance == .list {
                            YabaIconView(bundleKey: "tick-01")
                        }
                        Text(ViewType.list.getUITitle())
                    }
                } icon: {
                    YabaIconView(bundleKey: ViewType.list.getUIIconName())
                }
            }
            Menu {
                ForEach(CardViewTypeImageSizing.allCases, id: \.self) { sizing in
                    Button {
                        withAnimation {
                            preferredCardViewImageSizing = sizing
                            preferredContentAppearance = .card
                        }
                    } label: {
                        Label {
                            HStack {
                                if preferredContentAppearance == .card && preferredCardViewImageSizing == sizing {
                                    YabaIconView(bundleKey: "tick-01")
                                }
                                Text(sizing.getUITitle())
                            }
                        } icon: {
                            YabaIconView(bundleKey: sizing.getUIIconName())
                        }
                    }
                }
            } label: {
                Label {
                    HStack {
                        if preferredContentAppearance == .card {
                            YabaIconView(bundleKey: "tick-01")
                        }
                        Text(ViewType.card.getUITitle())
                    }
                } icon: {
                    YabaIconView(bundleKey: ViewType.card.getUIIconName())
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
