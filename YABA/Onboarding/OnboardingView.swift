//
//  OnboardingView.swift
//  YABA
//
//  Created by Ali Taha on 1.06.2025.
//

import SwiftUI

struct OnboardingView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: OnboardingState = .init()
    
    let onDone: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                AnimatedGradient(collectionColor: state.currentPage.getTint())
                mainContent
            }
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    YabaIconView(bundleKey: "arrow-left-01")
                        .frame(width: 24, height: 24)
                        .foregroundStyle(state.currentPage.getTint())
                        .opacity(state.currentPage == .welcome ? 0 : 1)
                        .animation(.smooth, value: state.currentPage == .welcome)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            state.previousPage()
                        }
                }
            }
            .overlay(alignment: .bottom) {
                stepButton
            }
            .tint(state.currentPage.getTint())
        }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        ScrollView(.horizontal) {
            LazyHStack {
                ForEach(OnboardingPage.allCases, id: \.self) { page in
                    page.getUIView(
                        shouldGenerate: $state.generationEnabled,
                        hasAlreadyGenerated: $state.generated,
                        onRequestButtonVisibility: { visible in
                            withAnimation {
                                state.showButton = visible
                            }
                        }
                    )
                    .id(page.self)
                    .containerRelativeFrame(.horizontal)
                    .scrollTargetLayout()
                    .scrollTransition { content, phase in
                        content
                            .opacity(phase.isIdentity ? 1 : 0)
                            .blur(radius: phase.isIdentity ? 0 : 10)
                    }
                }
            }
        }
        .scrollDisabled(true)
        .scrollTargetBehavior(.paging)
        .scrollContentBackground(.hidden)
        .scrollPosition(id: Binding<OnboardingPage?>(
            get: { state.currentPage },
            set: { newValue in
                if let newValue {
                    state.currentPage = newValue
                }
            }
        ))
    }
    
    @ViewBuilder
    private var stepButton: some View {
        Button {
            state.nextPage(
                andGenerateWith: modelContext,
                onDoneCallback: onDone
            )
        } label: {
            HStack {
                Text(state.currentPage.getUITitle(generationEnabled: state.generationEnabled))
                    .font(.title3)
                    .fontWeight(.medium)
                    .frame(
                        width: 300,
                        height: 56
                    )
                if state.isLoading {
                    ProgressView().controlSize(.small)
                }
            }
        }
        .buttonStyle(.borderedProminent)
        .tint(state.currentPage.getTint().opacity(0.3))
        .animation(.smooth, value: state.currentPage)
        .drawingGroup()
        .opacity(state.showButton ? 1 : 0)
        .animation(.smooth, value: state.showButton)
        .padding(.horizontal)
        .padding(.bottom)
    }
}
