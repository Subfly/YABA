//
//  HowToSync.swift
//  YABA
//
//  Created by Ali Taha on 11.09.2025.
//

import SwiftUI

struct HowToSync: View {
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .orange)
            List {
                #if targetEnvironment(macCatalyst)
                GeneralSync(width: 500)
                    .listRowSeparator(.hidden)
                #else
                if UIDevice.current.userInterfaceIdiom == .pad {
                    GeneralSync(width: 500)
                        .listRowSeparator(.hidden)
                } else {
                    GeneralSync(width: 350)
                        .listRowSeparator(.hidden)
                }
                #endif
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Sync Title")
    }
}

private struct GeneralSync: View {
    private let messages: [LocalizedStringKey] = [
    ]
    
    private let images: [UIImage] = [
    ]
    
    let width: CGFloat
    
    var body: some View {
        ForEach(Array(0...2), id: \.self) { step in
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
    HowToSync()
}
