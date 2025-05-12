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
    private let collectionColor: Color
    
    @State
    private var shouldAnimate: Bool = false
    
    init(collectionColor: Color) {
        self.collectionColor = collectionColor
    }
    
    var body: some View {
        LinearGradient(
            colors: [
                .clear, .clear, .clear,
                .clear, .clear, .clear,
                collectionColor.opacity(0.05), collectionColor.opacity(0.05), collectionColor.opacity(0.05),
                collectionColor.opacity(0.1), collectionColor.opacity(0.1), collectionColor.opacity(0.1),
                collectionColor.opacity(0.15), collectionColor.opacity(0.15), collectionColor.opacity(0.15),
                collectionColor.opacity(0.2), collectionColor.opacity(0.2), collectionColor.opacity(0.2),
                collectionColor.opacity(
                    shouldAnimate ? 0.3 : 0.25
                ), collectionColor.opacity(0.25), collectionColor.opacity(
                    shouldAnimate ? 0.3 : 0.25
                ),
                collectionColor.opacity(
                    shouldAnimate ? 0.25 : 0.3
                ), collectionColor.opacity(0.3), collectionColor.opacity(
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
            withAnimation(.easeInOut(duration: 3).repeatForever(autoreverses: true)) {
                shouldAnimate.toggle()
            }
        }
    }
}

#Preview {
    AnimatedGradient(
        collectionColor: .indigo
    )
}
