//
//  PromisesPage.swift
//  YABA
//
//  Created by Ali Taha on 2.06.2025.
//


import SwiftUI

private struct PromiseItem: Identifiable {
    let id = UUID()
    let title: LocalizedStringKey
    let detail: LocalizedStringKey
    
    static func items() -> [PromiseItem] {
        return [
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 1 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 1 Detail")
            ),
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 2 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 2 Detail")
            ),
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 3 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 3 Detail")
            ),
            /* IT IS STILL A PROMISE, BUT I HAD TO REMOVE IT TO FIT THE SCREEN :(
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 4 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 4 Detail")
            ),
             */
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 5 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 5 Detail")
            ),
            /* IT IS STILL A PROMISE, BUT I HAD TO REMOVE IT TO FIT THE SCREEN :(
            PromiseItem(
                title: LocalizedStringKey("Onboarding Promises Page Step 6 Title"),
                detail: LocalizedStringKey("Onboarding Promises Page Step 6 Detail")
            )
             */
        ]
    }
}

internal struct PromisesPage: View {
    @State private var shouldShowList: [Bool] = [false, false, false, false]
    @State private var hasShown: Bool = false
    
    private let promises = PromiseItem.items()
    let onRequestButtonVisibility: (Bool) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            ForEach(Array(promises.enumerated()), id: \.element.id) { index, item in
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
                        for index in promises.indices {
                            withAnimation {
                                shouldShowList[index] = true
                            }
                            try? await Task.sleep(for: .seconds(0.5))
                        }
                        onRequestButtonVisibility(true)
                        hasShown = true
                    }
                }
            } else {
                for index in promises.indices {
                    shouldShowList[index] = true
                }
                onRequestButtonVisibility(true)
                hasShown = true
            }
        }
    }
    
    @ViewBuilder
    private func generateItem(for item: PromiseItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.title)
                .font(.title3)
                .fontWeight(.semibold)
            
            Text(item.detail)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .padding(.trailing)
        .padding(.trailing)
    }
}
