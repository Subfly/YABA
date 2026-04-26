// ARCHIVED: Previous implementation preserved below (not compiled). UI rebuild in progress.

#if false
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
                if UIDevice.current.userInterfaceIdiom == .pad {
                    GeneralSync(width: 500)
                        .listRowSeparator(.hidden)
                } else {
                    GeneralSync(width: 350)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Sync Title")
    }
}

private struct GeneralSync: View {
    private let messages: [LocalizedStringKey] = [
        "How To Sync General Message 1",
        "How To Sync General Message 2",
        "How To Sync General Message 3",
        "How To Sync General Message 4",
        "How To Sync General Message 5",
        "How To Sync General Message 6",
        "How To Sync General Message 7",
        "How To Sync General Message 8"
    ]
    
    private let images: [UIImage] = [
        .howToSyncRename,
        .howToSyncStep1,
        .howToSyncPermission,
        .howToSyncStep2,
        .howToSyncStep3,
        .howToSyncStep4,
        .howToSyncStep5,
        .howToSyncSettings
    ]
    
    let width: CGFloat
    
    var body: some View {
        ForEach(Array(0...7), id: \.self) { step in
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

#endif
