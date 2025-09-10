//
//  HowToReminder.swift
//  YABA
//
//  Created by Ali Taha on 10.09.2025.
//

import SwiftUI

struct HowToReminders: View {
    let messages: [LocalizedStringKey] = [
        "How To Reminders General Message 1",
        "How To Reminders General Message 2",
        "How To Reminders General Message 3",
        "How To Reminders General Message 4",
        "How To Reminders General Message 5",
        "How To Reminders General Message 6"
    ]
    
    let images: [UIImage] = [
        .howToRemindersDetailOptions,
        .howToRemindersSetTime,
        .howToRemindersPermission,
        .howToRemindersCreated,
        .howToRemindersCancel,
        .howToRemindersExample
    ]
    
    var body: some View {
        ZStack {
            AnimatedGradient(collectionColor: .yellow)
            List {
                if UIDevice.current.userInterfaceIdiom == .phone {
                    generateContent(width: 350)
                        .listRowSeparator(.hidden)
                } else {
                    generateContent(width: 500)
                        .listRowSeparator(.hidden)
                }
            }
            .listStyle(.sidebar)
            .scrollContentBackground(.hidden)
        }.navigationTitle("How To Reminders Title")
    }
    
    @ViewBuilder
    private func generateContent(width: CGFloat) -> some View {
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
    HowToReminders()
}
