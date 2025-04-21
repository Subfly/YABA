//
//  AnimatedMeshGradient.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import SwiftUI

struct AnimatedMeshGradient: View {
    @Environment(\.colorScheme)
    private var scheme
    
    @State
    private var colors: [Color] = [
        .clear, .clear, .clear,
        .clear, .clear, .clear,
        .clear, .clear, .clear,
        .clear, .clear, .clear,
        .clear, .clear, .clear,
        .clear, .clear, .clear,
    ]
    
    private let timer = Timer.publish(
        every: 3,
        on: .current,
        in: .common
    ).autoconnect()
    
    private let first: Color
    private let second: Color
    private let third: Color
    private let fourth: Color
    private let fifth: Color
    private let sixth: Color
    
    let collectionColor: Color
    
    init(collectionColor: Color) {
        self.collectionColor = collectionColor
        self.first = collectionColor.opacity(0.05)
        self.second = collectionColor.opacity(0.1)
        self.third = collectionColor.opacity(0.15)
        self.fourth = collectionColor.opacity(0.2)
        self.fifth = collectionColor.opacity(0.3)
        self.sixth = collectionColor.opacity(0.4)
    }
    
    var body: some View {
        MeshGradient(
            width: 3,
            height: 6,
            points: [
                [0, 0], [0.5, 0], [1, 0],
                [0, 0.2], [0.5, 0.2], [1, 0.2],
                [0, 0.4], [0.5, 0.4], [1, 0.4],
                [0, 0.6], [0.5, 0.6], [1, 0.6],
                [0, 0.8], [0.5, 0.8], [1, 0.8],
                [0, 1], [0.5, 1], [1, 1]
            ],
            colors: colors,
        )
        .overlay(.ultraThinMaterial.opacity(0.9))
        .ignoresSafeArea()
        .onReceive(timer) { _ in
            let topColors = [
                first, first, first,
                second, second, second,
                third, third, third,
                fourth, fourth, fourth,
            ]
            let bottomColors = [
                fifth, fifth, fifth,
                sixth, sixth, sixth,
            ]
            withAnimation(.smooth) {
                colors = topColors + bottomColors.shuffled()
            }
        }
        .onAppear {
            withAnimation {
                colors = [
                    first, first, first,
                    second, second, second,
                    third, third, third,
                    fourth, fourth, fourth,
                    fifth, fifth, fifth,
                    sixth, sixth, sixth,
                ]
            }
        }
        .onChange(of: collectionColor) { _, _ in
            withAnimation {
                colors = [
                    first, first, first,
                    second, second, second,
                    third, third, third,
                    fourth, fourth, fourth,
                    fifth, fifth, fifth,
                    sixth, sixth, sixth,
                ]
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
