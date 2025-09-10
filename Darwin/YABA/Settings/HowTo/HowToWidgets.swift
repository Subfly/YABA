//
//  HowToWidgets.swift
//  YABA
//
//  Created by Ali Taha on 10.09.2025.
//

import SwiftUI

struct HowToWidgets: View {
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .indigo)
            List {
                #if targetEnvironment(macCatalyst)
                MacWidgets()
                    .listRowSeparator(.hidden)
                #else
                if UIDevice.current.userInterfaceIdiom == .pad {
                    GeneralWidgets(width: 500)
                        .listRowSeparator(.hidden)
                } else {
                    GeneralWidgets(width: 350)
                        .listRowSeparator(.hidden)
                }
                #endif
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Widgets Title")
    }
}

private struct MacWidgets: View {
    let messages: [LocalizedStringKey] = [
        "How To Widgets Mac Message 1",
        "How To Widgets Mac Message 2",
        "How To Widgets Mac Message 3",
        "How To Widgets Mac Message 4",
        "How To Widgets Mac Message 5"
    ]
    
    let images: [UIImage] = [
        .howToWidgetsMacAdd1,
        .howToWidgetsMacAdd2,
        .howToWidgetsMacEdit1,
        .howToWidgetsMacEdit2,
        .howToWidgetsMacEdit3
    ]
    
    var body: some View {
        ForEach(Array(0...4), id: \.self) { step in
            Section {
                Text(messages[step])
                HStack {
                    Spacer()
                    Image(uiImage: images[step])
                        .resizable()
                        .scaledToFill()
                        .frame(width: 500)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    Spacer()
                }
            } header: {
                Label {
                    Text("How To Step Label \(step+1)")
                } icon: {
                    YabaIconView(bundleKey: "grid")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
    }
}

private struct GeneralWidgets: View {
    let messages: [LocalizedStringKey] = [
        "How To Widgets General Message 1",
        "How To Widgets General Message 2",
        "How To Widgets General Message 3",
        "How To Widgets General Message 4",
        "How To Widgets General Message 5",
        "How To Widgets General Message 6"
    ]
    
    let images: [UIImage] = [
        .howToWidgetsGeneralAdd1,
        .howToWidgetsGeneralAdd2,
        .howToWidgetsGeneralAdd3,
        .howToWidgetsGeneralEdit1,
        .howToWidgetsGeneralEdit2,
        .howToWidgetsGeneralEdit3
    ]
    
    let width: CGFloat
    
    var body: some View {
        ForEach(Array(0...5), id: \.self) { step in
            Section {
                Text(messages[step])
                HStack {
                    Spacer()
                    Image(uiImage: images[step])
                        .resizable()
                        .scaledToFill()
                        .frame(width: width)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    Spacer()
                }
            } header: {
                Label {
                    Text("How To Step Label \(step+1)")
                } icon: {
                    YabaIconView(bundleKey: "grid")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
    }
}

#Preview {
    HowToWidgets()
}
