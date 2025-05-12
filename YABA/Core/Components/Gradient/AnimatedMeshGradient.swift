//
//  AnimatedMeshGradient.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//
//  Perfomance wise very shit...
// 

import SwiftUI

struct AnimatedMeshGradient: View {
    @State
    private var colors: [Color]
    
    private let collectionColor: Color
    private let constColors: [Color]
    private let randomizedColors: [Color]
    private let shade1: Color
    private let shade2: Color
    
    private let points: [SIMD2<Float>] = [
        [0, 0], [0.5, 0], [1, 0],
        [0, 0.2], [0.5, 0.2], [1, 0.2],
        [0, 0.4], [0.5, 0.4], [1, 0.4],
        [0, 0.6], [0.5, 0.6], [1, 0.6],
        [0, 0.8], [0.5, 0.8], [1, 0.8],
        [0, 1], [0.5, 1], [1, 1]
    ]
    
    private let timer = Timer.publish(
        every: 3,
        on: .current,
        in: .common
    ).autoconnect()
    
    init(collectionColor: Color) {
        let first = collectionColor.opacity(0.05)
        let second = collectionColor.opacity(0.1)
        let third = collectionColor.opacity(0.15)
        let fourth = collectionColor.opacity(0.2)
        self.shade1 = collectionColor.opacity(0.3)
        self.shade2 = collectionColor.opacity(0.4)
        
        constColors = [
            first, first, first,
            second, second, second,
            third, third, third,
            fourth, fourth, fourth,
        ]
        
        randomizedColors = [
            shade1, shade1, shade1,
            shade2, shade2, shade2,
        ]
        
        colors = constColors + randomizedColors
        self.collectionColor = collectionColor
    }
    
    var body: some View {
        MeshGradient(
            width: 3,
            height: 6,
            points: points,
            colors: colors,
        )
        .overlay(.ultraThinMaterial.opacity(0.9))
        .ignoresSafeArea()
        .onReceive(timer) { _ in
            withAnimation {
                colors = constColors + randomizedColors.shuffled()
            }
        }
        .onChange(of: collectionColor) {
            withAnimation {
                colors = constColors + randomizedColors.shuffled()
            }
        }
        .opacity(0.5)
    }
}

#Preview {
    AnimatedMeshGradient(
        collectionColor: .indigo
    )
}
