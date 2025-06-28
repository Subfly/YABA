//
//  HowToPage.swift
//  YABA
//
//  Created by Ali Taha on 2.06.2025.
//

import SwiftUI

private struct HowToItem: Identifiable {
    let id = UUID()
    let title: LocalizedStringKey
    let detail: LocalizedStringKey
    
    static func getItems() -> [HowToItem] {
        #if targetEnvironment(macCatalyst)
        return HowToItem.macItems()
        #else
        return HowToItem.generalItems()
        #endif
    }
    
    static func generalItems() -> [HowToItem] {
        return [
            .init(
                title: LocalizedStringKey("Onboarding HowTo Page General Step 1 Title"),
                detail: LocalizedStringKey("Onboarding HowTo Page General Step 1 Detail")
            ),
            .init(
                title: LocalizedStringKey("Onboarding HowTo Page General Step 2 Title"),
                detail: LocalizedStringKey("Onboarding HowTo Page General Step 2 Detail")
            ),
            .init(
                title: LocalizedStringKey("Onboarding HowTo Page General Step 3 Title"),
                detail: LocalizedStringKey("Onboarding HowTo Page General Step 3 Detail")
            ),
            .init(
                title: LocalizedStringKey("Onboarding HowTo Page Final Step Title"),
                detail: LocalizedStringKey("Onboarding HowTo Page Final Step Detail")
            )
        ]
    }
    
    static func macItems() -> [HowToItem] {
        return [
            .init(
                title: "Onboarding HowTo Page Mac Step 1 Title",
                detail: "Onboarding HowTo Page Mac Step 1 Detail"
            ),
            .init(
                title: "Onboarding HowTo Page Mac Step 2 Title",
                detail: "Onboarding HowTo Page Mac Step 2 Detail"
            ),
            .init(
                title: "Onboarding HowTo Page Mac Step 3 Title",
                detail: "Onboarding HowTo Page Mac Step 3 Detail"
            ),
            .init(
                title: "Onboarding HowTo Page Final Step Mac Title",
                detail: "Onboarding HowTo Page Final Step Mac Detail"
            )
        ]
    }
}

internal struct HowToPage: View {
    @State
    private var shouldShowList: [Bool] = [false, false, false, false]
    
    @State
    private var hasShown: Bool = false
    
    private let items: [HowToItem] = HowToItem.getItems()
    let onRequestButtonVisibility: (Bool) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                generateItem(for: item)
                    .opacity(shouldShowList[index] ? 1 : 0)
                    .animation(.smooth, value: shouldShowList[index])
            }
        }
        .multilineTextAlignment(.leading)
        .padding(.horizontal)
        .padding(.bottom)
        .padding(.bottom)
        .onAppear {
            if UIDevice.current.userInterfaceIdiom != .pad {
                Task {
                    if !hasShown {
                        onRequestButtonVisibility(false)
                        try? await Task.sleep(for: .seconds(0.5))
                        for index in items.indices {
                            withAnimation {
                                shouldShowList[index] = true
                            }
                            try? await Task.sleep(for: .seconds(1))
                        }
                        onRequestButtonVisibility(true)
                        hasShown = true
                    }
                }
            } else {
                for index in items.indices {
                    shouldShowList[index] = true
                }
                onRequestButtonVisibility(true)
                hasShown = true
            }
        }
    }
    
    @ViewBuilder
    private func generateItem(for item: HowToItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.title)
                .font(.title3)
                .fontWeight(.semibold)
            
            Text(item.detail)
                .font(.body)
                .foregroundStyle(.secondary)
        }.padding(.trailing)
    }
}
