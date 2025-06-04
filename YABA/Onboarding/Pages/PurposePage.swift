//
//  PurposePage.swift
//  YABA
//
//  Created by Ali Taha on 2.06.2025.
//

import SwiftUI

private struct FrustrationItem: Identifiable {
    let id = UUID()
    let main: LocalizedStringKey
    let subtext: LocalizedStringKey
    
    static func getItems() -> [FrustrationItem] {
        return [
            FrustrationItem(
                main: LocalizedStringKey("Onboarding Purpose Page First Item Main Text"),
                subtext: LocalizedStringKey("Onboarding Purpose Page First Item Sub Text"),
            ),
            FrustrationItem(
                main: LocalizedStringKey("Onboarding Purpose Page Second Item Main Text"),
                subtext: LocalizedStringKey("Onboarding Purpose Page Second Item Sub Text")
            ),
            FrustrationItem(
                main: LocalizedStringKey("Onboarding Purpose Page Third Item Main Text"),
                subtext: LocalizedStringKey("Onboarding Purpose Page Third Item Sub Text")
            )
        ]
    }
}

internal struct PurposePage: View {
    @State
    private var showFirstItem: Bool = false
    
    @State
    private var showSecondItem: Bool = false
    
    @State
    private var showThirdItem: Bool = false
    
    @State
    private var showMainItem: Bool = false
    
    @State
    private var hasShown: Bool = false
    
    private let items = FrustrationItem.getItems()
    let onRequestButtonVisibility: (Bool) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            if UIDevice.current.userInterfaceIdiom == .pad {
                HStack {
                    Spacer()
                    VStack(alignment: .leading, spacing: 28) {
                        generateItem(for: items[0])
                            .opacity(showFirstItem ? 1 : 0)
                            .animation(.smooth, value: showFirstItem)
                        generateItem(for: items[1])
                            .opacity(showSecondItem ? 1 : 0)
                            .animation(.smooth, value: showSecondItem)
                        generateItem(for: items[2])
                            .opacity(showThirdItem ? 1 : 0)
                            .animation(.smooth, value: showThirdItem)
                    }
                    Spacer()
                }
            } else {
                generateItem(for: items[0])
                    .opacity(showFirstItem ? 1 : 0)
                    .animation(.smooth, value: showFirstItem)
                generateItem(for: items[1])
                    .opacity(showSecondItem ? 1 : 0)
                    .animation(.smooth, value: showSecondItem)
                generateItem(for: items[2])
                    .opacity(showThirdItem ? 1 : 0)
                    .animation(.smooth, value: showThirdItem)
            }
            
            Spacer().frame(height: 28)

            HStack {
                Spacer()
                Text("Onboarding Purpose Page Main Text")
                    .font(.title)
                    .fontWeight(.bold)
                    .opacity(showMainItem ? 1 : 0)
                    .animation(.smooth, value: showMainItem)
                Spacer()
            }
        }
        .multilineTextAlignment(.leading)
        .frame(
            width: UIDevice.current.userInterfaceIdiom == .vision
            ? 600
            : nil
        )
        .padding(.horizontal)
        .padding(.bottom)
        .padding(.bottom)
        .onAppear {
            if UIDevice.current.userInterfaceIdiom != .pad {
                Task {
                    if !hasShown {
                        onRequestButtonVisibility(false)
                        try? await Task.sleep(for: .seconds(0.5))
                        showFirstItem = true
                        try? await Task.sleep(for: .seconds(1))
                        showSecondItem = true
                        try? await Task.sleep(for: .seconds(1))
                        showThirdItem = true
                        try? await Task.sleep(for: .seconds(1))
                        showMainItem = true
                        try? await Task.sleep(for: .seconds(1))
                        onRequestButtonVisibility(true)
                        hasShown = true
                    }
                }
            } else {
                showFirstItem = true
                showSecondItem = true
                showThirdItem = true
                showMainItem = true
                onRequestButtonVisibility(true)
                hasShown = true
            }
        }
    }
    
    @ViewBuilder
    private func generateItem(for item: FrustrationItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.main)
                .font(.title3)
                .fontWeight(.semibold)

            Text(item.subtext)
                .font(.body)
                .foregroundStyle(.secondary)
        }.padding(.trailing)
    }
}
