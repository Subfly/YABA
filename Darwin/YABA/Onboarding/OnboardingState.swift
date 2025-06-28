//
//  OnboardingState.swift
//  YABA
//
//  Created by Ali Taha on 2.06.2025.
//

import Foundation
import SwiftUI
import SwiftData

internal enum OnboardingPage: Int, Hashable, CaseIterable {
    case welcome, purpose, howTo, promises, oneLastThing, done
    
    func getUITitle(generationEnabled: Bool) -> LocalizedStringKey {
        return switch self {
        case .welcome: LocalizedStringKey("Onboarding Start Button Label")
        case .purpose: LocalizedStringKey("Onboarding Purpose Button Label")
        case .howTo: LocalizedStringKey("Onboarding How To Button Label")
        case .promises: LocalizedStringKey("Onboarding Promises Button Label")
        case .oneLastThing: generationEnabled
            ? LocalizedStringKey("Onboarding One Last Thing Button Generate Label")
            : LocalizedStringKey("Onboarding One Last Thing Button Skip Label")
        case .done: LocalizedStringKey("Onboarding Done Button Label")
        }
    }
    
    @ViewBuilder
    func getUIView(
        shouldGenerate: Binding<Bool>,
        hasAlreadyGenerated: Binding<Bool>,
        onRequestButtonVisibility: @escaping (Bool) -> Void
    ) -> some View {
        switch self {
        case .welcome: WelcomePage(onRequestButtonVisibility: onRequestButtonVisibility)
        case .purpose: PurposePage(onRequestButtonVisibility: onRequestButtonVisibility)
        case .howTo: HowToPage(onRequestButtonVisibility: onRequestButtonVisibility)
        case .promises: PromisesPage(onRequestButtonVisibility: onRequestButtonVisibility)
        case .oneLastThing: GenerationPage(
            shouldGenerate: shouldGenerate,
            hasAlreadyGenerated: hasAlreadyGenerated,
            onRequestButtonVisibility: onRequestButtonVisibility
        )
        case .done: DonePage(onRequestButtonVisibility: onRequestButtonVisibility)
        }
    }
    
    func getTint() -> Color {
        return switch self {
        case .welcome: .accentColor
        case .purpose: .orange
        case .howTo: .red
        case .promises: .indigo
        case .oneLastThing: .purple
        case .done: .green
        }
    }
}

@MainActor
@Observable
internal class OnboardingState {
    var currentPage: OnboardingPage = .welcome
    var generationEnabled: Bool = false
    var generated: Bool = false
    var showButton: Bool = false
    var isLoading: Bool = false
    
    func nextPage(
        andGenerateWith modelContext: ModelContext,
        onDoneCallback: @escaping () -> Void
    ) {
        if currentPage == .done {
            onDoneCallback()
            return
        }
        
        if currentPage == .oneLastThing && generationEnabled && !generated {
            isLoading = true
            generateContent(with: modelContext)
            generated = true
            isLoading = false
        }
        
        withAnimation(.smooth) {
            if currentPage != .done {
                currentPage = switch currentPage {
                case .welcome: .purpose
                case .purpose: .howTo
                case .howTo: .promises
                case .promises: .oneLastThing
                case .oneLastThing: .done
                case .done: .welcome
                }
            }
        }
    }
    
    func previousPage() {
        withAnimation(.smooth) {
            if currentPage != .welcome {
                currentPage = switch currentPage {
                case .welcome: .done
                case .purpose: .welcome
                case .howTo: .purpose
                case .promises: .howTo
                case .oneLastThing: .promises
                case .done: .oneLastThing
                }
            }
        }
    }
    
    private func generateContent(with modelContext: ModelContext) {
        let collections = readJson()
        
        if !collections.isEmpty {
            collections.forEach { collection in
                modelContext.insert(collection)
            }
            try? modelContext.save()
        }
    }
    
    private func readJson() -> [YabaCollection] {
        guard let url = Bundle.main.url(forResource: "preload_data", withExtension: "json") else {
            return []
        }
        
        guard let data = try? Data(contentsOf: url),
           let decoded = try? JSONDecoder().decode(PreloadDataHolder.self, from: data) else {
            return []
        }
        
        return decoded.allCollections()
    }
}
