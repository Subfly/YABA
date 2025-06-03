//
//  GenerationPage.swift
//  YABA
//
//  Created by Ali Taha on 3.06.2025.
//

import SwiftUI

internal struct GenerationPage: View {
    @State
    private var shouldShowTitle: Bool = false
    
    @State
    private var shouldShowDescription: Bool = false
    
    @State
    private var shouldShowRest: Bool = false
    
    @State
    private var hasShown: Bool = false
    
    @Binding
    var shouldGenerate: Bool
    
    @Binding
    var hasAlreadyGenerated: Bool
    
    let onRequestButtonVisibility: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            Text("Onboarding Generation Page Title")
                .font(.title)
                .fontWeight(.medium)
                .opacity(shouldShowTitle ? 1 : 0)
                .animation(.smooth, value: shouldShowTitle)

            Text("Onboarding Generation Page Description")
                .font(.body)
                .foregroundStyle(.secondary)
                .opacity(shouldShowDescription ? 1 : 0)
                .animation(.smooth, value: shouldShowDescription)

            Toggle("Onboarding Generation Page Toggle Label", isOn: $shouldGenerate)
                .disabled(hasAlreadyGenerated)
                .opacity(shouldShowRest ? 1 : 0)
                .animation(.smooth, value: shouldShowRest)

            Text("Onboarding Generation Page Warning")
                .font(.footnote)
                .foregroundStyle(.tertiary)
                .opacity(shouldShowRest ? 1 : 0)
                .animation(.smooth, value: shouldShowRest)
        }
        .padding(.bottom)
        .padding(.bottom)
        .padding(.trailing, 56)
        .multilineTextAlignment(.leading)
        .onAppear {
            Task {
                if !hasShown {
                    onRequestButtonVisibility(false)
                    try? await Task.sleep(for: .seconds(0.5))
                    shouldShowTitle = true
                    try? await Task.sleep(for: .seconds(1))
                    shouldShowDescription = true
                    try? await Task.sleep(for: .seconds(1))
                    shouldShowRest = true
                    try? await Task.sleep(for: .seconds(1))
                    onRequestButtonVisibility(true)
                    hasShown = true
                }
            }
        }
    }
}
