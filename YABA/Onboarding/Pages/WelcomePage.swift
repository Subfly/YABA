//
//  WelcomePage.swift
//  YABA
//
//  Created by Ali Taha on 2.06.2025.
//

import SwiftUI

internal struct WelcomePage: View {
    @State
    private var shouldShowImage: Bool = false
    
    @State
    private var shouldShowTitle: Bool = false
    
    @State
    private var shouldShowMessage: Bool = false
    
    @State
    private var hasShown: Bool = false
    
    let onRequestButtonVisibility: (Bool) -> Void
    
    var body: some View {
        VStack {
            Image(.appIcon)
                .resizable()
                .scaledToFill()
                .frame(
                    width: UIDevice.current.userInterfaceIdiom == .pad
                    ? 300
                    : 200,
                    height: UIDevice.current.userInterfaceIdiom == .pad
                    ? 300
                    : 200,
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .clipped()
                .opacity(shouldShowImage ? 1 : 0)
                .animation(.smooth, value: shouldShowImage)
            
            Text("Onboarding Welcome Page Title")
                .font(.largeTitle)
                .fontWeight(.semibold)
                .padding()
                .padding(.top)
                .padding(.top)
                .opacity(shouldShowTitle ? 1 : 0)
                .animation(.smooth, value: shouldShowTitle)
            
            Text("Onboarding Welcome Page Message")
                .font(.title3)
                .foregroundStyle(.secondary)
                .opacity(shouldShowMessage ? 1 : 0)
                .animation(.smooth, value: shouldShowMessage)
            
            Spacer().frame(height: 100)
        }
        .onAppear {
            if UIDevice.current.userInterfaceIdiom != .pad {
                Task {
                    if !hasShown {
                        onRequestButtonVisibility(false)
                        try? await Task.sleep(for: .seconds(0.5))
                        shouldShowImage = true
                        try? await Task.sleep(for: .seconds(1))
                        shouldShowTitle = true
                        try? await Task.sleep(for: .seconds(1))
                        shouldShowMessage = true
                        try? await Task.sleep(for: .seconds(1))
                        onRequestButtonVisibility(true)
                        hasShown = true
                    }
                }
            } else {
                shouldShowImage = true
                shouldShowTitle = true
                shouldShowMessage = true
                onRequestButtonVisibility(true)
                hasShown = true
            }
        }
    }
}
