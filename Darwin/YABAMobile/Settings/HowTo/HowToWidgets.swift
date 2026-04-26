// ARCHIVED: Previous implementation preserved below (not compiled). UI rebuild in progress.

#if false
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
                if UIDevice.current.userInterfaceIdiom == .pad {
                    GeneralWidgets(width: 500)
                        .listRowSeparator(.hidden)
                } else {
                    GeneralWidgets(width: 350)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Widgets Title")
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

#endif
