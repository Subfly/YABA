//
//  HowToGuideView.swift
//  YABA
//
//  Created by Ali Taha on 9.09.2025.
//

import SwiftUI

private enum HowToGuidePage {
    case share, keyboard, widgets, sync, reminders, tips
    
    func getTitle() -> LocalizedStringKey {
        switch self {
        case .share: "How To Share Title"
        case .keyboard: "How To Keyboard Title"
        case .widgets: "How To Widgets Title"
        case .sync: "How To Sync Title"
        case .reminders: "How To Reminders Title"
        case .tips: "How To Tips Title"
        }
    }
    
    func getUIIconName() -> String {
        switch self {
        case .share: "share-03"
        case .keyboard: "keyboard"
        case .widgets: "rectangular"
        case .sync: "laptop-phone-sync"
        case .reminders: "notification-01"
        case .tips: "sparkles"
        }
    }
    
    func getColor() -> Color {
        switch self {
        case .share: .blue
        case .keyboard: .teal
        case .widgets: .indigo
        case .sync: .orange
        case .reminders: .yellow
        case .tips: .green
        }
    }
}

struct HowToGuideView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var path: [HowToGuidePage] = []
    
    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                List {
                    #if !targetEnvironment(macCatalyst)
                    generateHowToItem(for: .keyboard)
                    #endif
                    generateHowToItem(for: .reminders)
                    generateHowToItem(for: .share)
                    generateHowToItem(for: .sync)
                    generateHowToItem(for: .tips)
                    generateHowToItem(for: .widgets)
                }
                .listStyle(.sidebar)
                .listRowSpacing(12)
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Settings How To Guide Title")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Text("Done")
                    }
                }
            }
            .navigationDestination(for: HowToGuidePage.self) { destination in
                switch destination {
                case .share: HowToShare()
                case .keyboard: HowToKeyboard()
                case .widgets: HowToWidgets()
                case .sync: HowToSync()
                case .reminders: HowToReminders()
                case .tips: EmptyView()
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }
    
    @ViewBuilder
    private func generateHowToItem(for page: HowToGuidePage) -> some View {
        NavigationLink(value: page) {
            Label {
                Text(page.getTitle())
            } icon: {
                YabaIconView(bundleKey: page.getUIIconName())
                    .foregroundStyle(page.getColor())
                    .frame(width: 24, height: 24)
            }
        }
    }
}

#Preview {
    HowToGuideView()
}
