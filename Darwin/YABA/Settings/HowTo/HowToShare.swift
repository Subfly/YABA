//
//  HowToShare.swift
//  YABA
//
//  Created by Ali Taha on 9.09.2025.
//

import SwiftUI

struct HowToShare: View {
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .blue)
            List {
                #if targetEnvironment(macCatalyst)
                MacShare()
                    .listRowSeparator(.hidden)
                #else
                if UIDevice.current.userInterfaceIdiom == .pad {
                    iPadShare()
                        .listRowSeparator(.hidden)
                } else {
                    iPhoneShare()
                        .listRowSeparator(.hidden)
                }
                #endif
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Share Title")
    }
}

private struct MacShare: View {
    private let messages: [LocalizedStringKey] = [
        "How To Share Mac Message 1",
        "How To Share Mac Message 2",
        "How To Share Mac Message 3"
    ]
    
    private let images: [UIImage] = [
        .howToShareMacQuickKey,
        .howToShareMacMenuBar,
        .howToShareMacSettings
    ]
    
    var body: some View {
        ForEach(Array(0...2), id: \.self) { step in
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

private struct iPadShare: View {
    private let messages: [LocalizedStringKey] = [
        "How To Share General Message 1",
        "How To Share General Message 2",
        "How To Share General Message 3",
        "How To Share General Message 4",
        "How To Share General Message 5",
        "How To Share General Message 6",
        "How To Share General Message 7",
        "How To Share General Message 8",
        "How To Share General Message 9"
    ]
    
    private let images: [UIImage] = [
        .howToShareIpadYabaExists,
        .howToShareIpadYabaDne,
        .howToShareIpadAddYaba,
        .howToShareIpadMove,
        .howToShareIpadRegularSheet,
        .howToShareIpadSettings,
        .howToShareIpadSimplifiedSheet,
        .howToShareIpadHidden1,
        .howToShareIpadHidden2
    ]
    
    var body: some View {
        ForEach(Array(0...8), id: \.self) { step in
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

private struct iPhoneShare: View {
    private let messages: [LocalizedStringKey] = [
        "How To Share General Message 1",
        "How To Share General Message 2",
        "How To Share General Message 3",
        "How To Share General Message 4",
        "How To Share General Message 5",
        "How To Share General Message 6",
        "How To Share General Message 7",
        "How To Share General Message 8",
        "How To Share General Message 9"
    ]
    
    private let images: [UIImage] = [
        .howToShareIphoneYabaExists,
        .howToShareIphoneYabaDne,
        .howToShareIphoneAddYaba,
        .howToShareIphoneMove,
        .howToShareIphoneRegularSheet,
        .howToShareIphoneSettings,
        .howToShareIphoneSimplifiedSheet,
        .howToShareIphoneHidden1,
        .howToShareIphoneHidden2
    ]
    
    var body: some View {
        ForEach(Array(0...8), id: \.self) { step in
            Section {
                Text(messages[step])
                HStack {
                    Spacer()
                    Image(uiImage: images[step])
                        .resizable()
                        .scaledToFill()
                        .frame(width: 350)
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
    HowToShare()
}
