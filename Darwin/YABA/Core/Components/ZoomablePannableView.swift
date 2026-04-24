import SwiftUI

/// Zoom + pan behavior aligned with Compose imagemark detail.
struct ZoomablePannableView<Content: View>: View {
    private let minScale: CGFloat = 1.0
    private let doubleTapScale: CGFloat = 2.5
    private let scaleEpsilon: CGFloat = 0.001

    @State private var baseScale: CGFloat = 1.0
    @State private var baseOffset: CGSize = .zero
    @State private var pinchScale: CGFloat = 1.0
    @State private var dragOffset: CGSize = .zero

    private let maxScale: CGFloat
    private let content: () -> Content

    init(maxScale: CGFloat = 5.0, @ViewBuilder content: @escaping () -> Content) {
        self.maxScale = max(1.0, maxScale)
        self.content = content
    }

    var body: some View {
        GeometryReader { geometry in
            let liveScale = clampedScale(baseScale * pinchScale)
            let proposedOffset = CGSize(
                width: baseOffset.width + dragOffset.width,
                height: baseOffset.height + dragOffset.height
            )
            let liveOffset = boundedOffset(proposedOffset, container: geometry.size, scale: liveScale)

            content()
                .frame(width: geometry.size.width, height: geometry.size.height, alignment: .center)
                .scaleEffect(liveScale, anchor: .center)
                .offset(liveOffset)
                .frame(width: geometry.size.width, height: geometry.size.height, alignment: .center)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            dragOffset = value.translation
                        }
                        .onEnded { _ in
                            baseOffset = liveOffset
                            dragOffset = .zero
                        }
                        .simultaneously(with: MagnificationGesture()
                            .onChanged { value in
                                pinchScale = value
                            }
                            .onEnded { _ in
                                baseScale = liveScale
                                baseOffset = boundedOffset(
                                    CGSize(
                                        width: baseOffset.width + dragOffset.width,
                                        height: baseOffset.height + dragOffset.height
                                    ),
                                    container: geometry.size,
                                    scale: liveScale
                                )
                                pinchScale = 1.0
                                dragOffset = .zero
                            }
                        )
                )
                .onTapGesture(count: 2) {
                    withAnimation(.easeInOut(duration: 0.22)) {
                        if baseScale > minScale + scaleEpsilon {
                            baseScale = minScale
                            baseOffset = .zero
                        } else {
                            baseScale = clampedScale(doubleTapScale)
                            baseOffset = boundedOffset(baseOffset, container: geometry.size, scale: baseScale)
                        }
                        pinchScale = 1.0
                        dragOffset = .zero
                    }
                }
        }
    }

    private func clampedScale(_ value: CGFloat) -> CGFloat {
        min(max(value, minScale), maxScale)
    }

    private func boundedOffset(_ proposed: CGSize, container: CGSize, scale: CGFloat) -> CGSize {
        let contentWidth = container.width * scale
        let contentHeight = container.height * scale

        let maxOffsetX = max(0, (contentWidth - container.width) / 2)
        let maxOffsetY = max(0, (contentHeight - container.height) / 2)

        let boundedX = min(maxOffsetX, max(-maxOffsetX, proposed.width))
        let boundedY = min(maxOffsetY, max(-maxOffsetY, proposed.height))

        return CGSize(width: boundedX, height: boundedY)
    }
}
