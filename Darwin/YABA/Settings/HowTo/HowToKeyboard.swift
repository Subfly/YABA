//
//  HowToKeyboard.swift
//  YABA
//
//  Created by Ali Taha on 10.09.2025.
//

import SwiftUI

struct HowToKeyboard: View {
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .teal)
            List {
                if UIDevice.current.userInterfaceIdiom == .pad {
                    iPadKeyboard()
                        .listRowSeparator(.hidden)
                } else {
                    iPhoneKeyboard()
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Keyboard Title")
    }
}

private struct iPadKeyboard: View {
    private let messages: [LocalizedStringKey] = [
        "How To Keyboard General Message 1",
        "How To Keyboard General Message 2",
        "How To Keyboard General Message 3",
        "How To Keyboard General Message 4",
        "How To Keyboard General Message 5",
        "How To Keyboard General Message 6",
        "How To Keyboard General Message 7",
        "How To Keyboard General Message 8",
        "How To Keyboard General Message 9",
        "How To Keyboard General Message 10",
        "How To Keyboard General Message 11",
        "How To Keyboard General Message 12"
    ]
    
    private let images: [UIImage] = [
        .howToKeyboardIpadYabaSettings1,
        .howToKeyboardIpadYabaSettings2,
        .howToKeyboardIpadYabaSettings3,
        .howToKeyboardIpadYabaSettings4,
        .howToKeyboardIpadPadSettings1,
        .howToKeyboardIpadPadSettings2,
        .howToKeyboardIpadPadSettings3,
        .howToKeyboardIpadPadSettings4,
        .howToKeyboardIpadPadSettings5,
        .howToKeyboardIpadKeyboard1,
        .howToKeyboardIpadKeyboard2
    ]
    
    var body: some View {
        ForEach(Array(0...10), id: \.self) { step in
            Section {
                Text(messages[step])
                if step == 10 {
                    Text(messages[11])
                }
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

private struct iPhoneKeyboard: View {
    private let messages: [LocalizedStringKey] = [
        "How To Keyboard General Message 1",
        "How To Keyboard General Message 2",
        "How To Keyboard General Message 3",
        "How To Keyboard General Message 4",
        "How To Keyboard General Message 5",
        "How To Keyboard General Message 6",
        "How To Keyboard General Message 7",
        "How To Keyboard General Message 8",
        "How To Keyboard General Message 9",
        "How To Keyboard General Message 10",
        "How To Keyboard General Message 11",
        "How To Keyboard General Message 12"
    ]
    
    private let images: [UIImage] = [
        .howToKeyboardIphoneYabaSettings1,
        .howToKeyboardIphoneYabaSettings2,
        .howToKeyboardIphoneYabaSettings3,
        .howToKeyboardIphoneYabaSettings4,
        .howToKeyboardIphonePhoneSettings1,
        .howToKeyboardIphonePhoneSettings2,
        .howToKeyboardIphonePhoneSettings3,
        .howToKeyboardIphonePhoneSettings4,
        .howToKeyboardIphonePhoneSettings5,
        .howToKeyboardIphoneKeyboard1,
        .howToKeyboardIphoneKeyboard2,
        .howToKeyboardIphoneKeyboard3
    ]
    
    var body: some View {
        ForEach(Array(0...11), id: \.self) { step in
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
    HowToKeyboard()
}
