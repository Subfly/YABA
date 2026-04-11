//
//  AnimatedGradient.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//
//  Perfomance wise very shit...
//

import SwiftUI

struct AnimatedGradient: View {
    @AppStorage(Constants.disableBackgroundAnimationKey)
    private var disableBackgroundAnimation: Bool = false
    
    private let color: Color
    
    @State
    private var shouldAnimate: Bool = false
    
    init(color: Color) {
        self.color = color
    }
    
    var body: some View {
        LinearGradient(
            colors: [
                .clear, .clear, .clear,
                .clear, .clear, .clear,
                color.opacity(0.05), color.opacity(0.05), color.opacity(0.05),
                color.opacity(0.1), color.opacity(0.1), color.opacity(0.1),
                color.opacity(0.15), color.opacity(0.15), color.opacity(0.15),
                color.opacity(0.2), color.opacity(0.2), color.opacity(0.2),
                color.opacity(
                    shouldAnimate ? 0.3 : 0.25
                ), color.opacity(0.25), color.opacity(
                    shouldAnimate ? 0.3 : 0.25
                ),
                color.opacity(
                    shouldAnimate ? 0.25 : 0.3
                ), color.opacity(0.3), color.opacity(
                    shouldAnimate ? 0.25 : 0.3
                ),
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .overlay(.ultraThinMaterial.opacity(0.9))
        .ignoresSafeArea()
        .opacity(0.5)
        .blur(radius: 10)
        .onAppear {
            if !disableBackgroundAnimation {
                withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) {
                    shouldAnimate.toggle()
                }
            }
        }
    }
}
