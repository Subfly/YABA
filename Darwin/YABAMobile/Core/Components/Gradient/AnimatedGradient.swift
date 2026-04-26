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

    init(color: Color) {
        self.color = color
    }
    
    var body: some View {
        Group {
            if disableBackgroundAnimation {
                powderedGradient(phase: 0)
            } else {
                TimelineView(.animation(minimumInterval: 1.0 / 45.0)) { context in
                    powderedGradient(
                        phase: context.date.timeIntervalSinceReferenceDate
                    )
                }
            }
        }
        .overlay(.ultraThinMaterial.opacity(0.9))
        .ignoresSafeArea()
        .opacity(0.5)
        .blur(radius: 14)
    }
}

private extension AnimatedGradient {
    @ViewBuilder
    func powderedGradient(phase: TimeInterval) -> some View {
        let points = animatedPoints(phase: phase)
        let colors = animatedColors(phase: phase)

        MeshGradient(
            width: 3,
            height: 3,
            points: points,
            colors: colors,
            background: .clear,
            smoothsColors: true
        )
        .drawingGroup(opaque: false, colorMode: .linear)
    }

    func animatedPoints(phase: TimeInterval) -> [SIMD2<Float>] {
        let t = Float(phase)

        func wave(_ seed: Float, amount: Float) -> Float {
            sin(t * 0.16 + seed) * amount
        }

        func clamp(_ value: Float) -> Float {
            min(max(value, 0), 1)
        }

        return [
            SIMD2<Float>(0.0, 0.0),
            SIMD2<Float>(clamp(0.5 + wave(0.3, amount: 0.1)), 0.0),
            SIMD2<Float>(1.0, 0.0),
            SIMD2<Float>(0.0, clamp(0.5 + wave(1.1, amount: 0.14))),
            SIMD2<Float>(
                clamp(0.5 + wave(2.2, amount: 0.12)),
                clamp(0.5 + wave(3.4, amount: 0.12))
            ),
            SIMD2<Float>(1.0, clamp(0.5 + wave(4.1, amount: 0.14))),
            SIMD2<Float>(0.0, 1.0),
            SIMD2<Float>(clamp(0.5 + wave(5.3, amount: 0.1)), 1.0),
            SIMD2<Float>(1.0, 1.0),
        ]
    }

    func animatedColors(phase: TimeInterval) -> [Color] {
        let t = Float(phase)
        let pulseA = 0.27 + (sin(t * 0.35) * 0.035)
        let pulseB = 0.22 + (cos(t * 0.33) * 0.03)

        return [
            .clear, .clear, .clear,
            .clear, color.opacity(Double(pulseB)), .clear,
            color.opacity(0.08), color.opacity(Double(pulseA)), color.opacity(0.18),
        ]
    }
}
