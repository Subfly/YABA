//
//  DonePage.swift
//  YABA
//
//  Created by Ali Taha on 3.06.2025.
//

import SwiftUI

internal struct DonePage: View {
    @State
    private var shouldShowTitle: Bool = false
    
    @State
    private var shouldShowMessage: Bool = false
    
    @State
    private var hasShown: Bool = false
    
    let onRequestButtonVisibility: (Bool) -> Void
    
    var body: some View {
        VStack(spacing: 32) {
            Text("Onboarding Done Title")
                .font(.largeTitle)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .opacity(shouldShowTitle ? 1 : 0)
                .animation(.smooth, value: shouldShowTitle)

            Text("Onboarding Done Message")
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .opacity(shouldShowMessage ? 1 : 0)
                .animation(.smooth, value: shouldShowMessage)
        }
        .padding(.horizontal)
        .padding(.bottom)
        .padding(.bottom)
        .onAppear {
            if UIDevice.current.userInterfaceIdiom != .pad {
                Task {
                    if !hasShown {
                        onRequestButtonVisibility(false)
                        try? await Task.sleep(for: .seconds(0.5))
                        shouldShowTitle = true
                        try? await Task.sleep(for: .seconds(0.5))
                        shouldShowMessage = true
                        try? await Task.sleep(for: .seconds(0.5))
                        onRequestButtonVisibility(true)
                        hasShown = true
                    }
                }
            } else {
                shouldShowTitle = true
                shouldShowMessage = true
                onRequestButtonVisibility(true)
                hasShown = true
            }
        }
    }
}
