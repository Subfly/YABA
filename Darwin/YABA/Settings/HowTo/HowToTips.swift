//
//  HowToTips.swift
//  YABA
//
//  Created by Ali Taha on 11.09.2025.
//

import SwiftUI

struct HowToTips: View {
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .green)
            List {
                #if targetEnvironment(macCatalyst)
                GeneralTips(width: 500)
                    .listRowSeparator(.hidden)
                #else
                if UIDevice.current.userInterfaceIdiom == .pad {
                    GeneralTips(width: 500)
                        .listRowSeparator(.hidden)
                } else {
                    GeneralTips(width: 350)
                        .listRowSeparator(.hidden)
                }
                #endif
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Tips Title")
    }
}

private struct GeneralTips: View {
    private let messages: [LocalizedStringKey] = [
        "How To Tips Message 1",
        "How To Tips Message 2",
        "How To Tips Message 3",
        "How To Tips Message 4",
        "How To Tips Message 5"
    ]
    
    private let images: [UIImage] = [
        .howToTipsSwipeToOptions,
        .howToTipsHoldForOptions,
        .howToTipsDeleteAnnouncement,
        .howToTipsControlCenter,
        .howToTipsResize
    ]
    
    let width: CGFloat
    
    var body: some View {
        ForEach(Array(0...4), id: \.self) { step in
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
                    Text("How To Tip Label \(step+1)")
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
    HowToTips()
}
