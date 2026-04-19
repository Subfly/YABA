//
//  ConentAppearancePicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

struct ContentAppearancePicker: View {
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var preferredContentAppearance: ContentAppearance = .list
    
    @AppStorage(Constants.preferredCardImageSizingKey)
    private var preferredCardViewImageSizing: CardImageSizing = .small
    
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
                        Text(ContentAppearance.list.getUITitle())
                    }
                } icon: {
                    YabaIconView(bundleKey: ContentAppearance.list.getUIIconName())
                }
            }
            Menu {
                ForEach(CardImageSizing.allCases, id: \.self) { sizing in
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
                        Text(ContentAppearance.card.getUITitle())
                    }
                } icon: {
                    YabaIconView(bundleKey: ContentAppearance.card.getUIIconName())
                }
            }
            Button {
                withAnimation {
                    preferredContentAppearance = .grid
                }
            } label: {
                Label {
                    HStack {
                        if preferredContentAppearance == .grid {
                            YabaIconView(bundleKey: "tick-01")
                        }
                        Text(ContentAppearance.grid.getUITitle())
                    }
                } icon: {
                    YabaIconView(bundleKey: ContentAppearance.grid.getUIIconName())
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
