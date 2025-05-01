//
//  YABAToast.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//


import SwiftUI

struct YABAToast: View {
    @State
    private var offset = CGFloat.zero
    
    let state: ToastState
    let onDismissRequest: () -> Void
    
    var body: some View {
        HStack(spacing: 0) {
            ZStack {
                colorBox.frame(
                    minWidth: 12,
                    maxWidth: state.iconType != .none ? .infinity : 12
                )
                if self.state.iconType != .none {
                    Image(systemName: state.iconType.getIcon())
                        .renderingMode(.template)
                        .tint(state.contentColor ?? .primary)
                }
            }
            .frame(
                maxWidth: state.iconType != .none ? 40 : 12,
                maxHeight: 60
            )
            ZStack {
                Color(UIColor.secondarySystemBackground)
                    .clipShape(
                        .rect(
                            topLeadingRadius: 0,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 16,
                            topTrailingRadius: 16
                        )
                    )
                HStack {
                    Text(state.message)
                        .fixedSize(horizontal: false, vertical: true)
                        .foregroundStyle(.white)
                        .padding()
                    
                    if let accepText = state.acceptText {
                        Spacer()
                        Button {
                            guard (state.onAcceptPressed != nil) else {
                                return
                            }
                            state.onAcceptPressed!()
                        } label: {
                            Text(accepText)
                                .foregroundStyle(state.accentColor ?? .accentColor)
                        }
                        .padding(.trailing)
                    }
                }
            }
        }
        .frame(
            maxWidth: UIDevice.current.userInterfaceIdiom == .phone ? nil : 400,
            maxHeight: 60,
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
        .offset(y: offset)
        .shadow(radius: 12)
        .gesture(
            DragGesture()
                .onChanged { gesture in
                    offset = gesture.translation.height
                }
                .onEnded { _ in
                    if abs(offset) > 10 {
                        onDismissRequest()
                    }
                    Task {
                        try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
                        offset = CGFloat.zero
                    }
                }
        )
    }
    
    @ViewBuilder
    private var colorBox: some View {
        if state.accentColor != nil {
            state.accentColor
        } else {
            Color.primary
        }
    }
}

#Preview {
    YABAToast(
        state: ToastState(
            accentColor: .green,
            iconType: .success
        ),
        onDismissRequest: {}
    )
}
