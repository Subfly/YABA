//
//  ToastModifier.swift
//  YABA
//
//  Created by Ali Taha on 4.06.2025.
//


import SwiftUI

struct ToastModifier: ViewModifier {
    let toastState: ToastState
    let isShowing: Bool
    let onDismiss: () -> Void

    func body(content: Content) -> some View {
        content
            .overlay(
                alignment: toastState.position == .top ? .top : .bottom
            ) {
                YabaToast(
                    state: toastState,
                    onDismissRequest: onDismiss
                )
                .offset(
                    y: toastState.position == .top
                        ? (isShowing ? 25 : -1000)
                        : (isShowing ? -25 : 1000)
                )
                .transition(
                    .move(edge: toastState.position == .top ? .top : .bottom)
                )
            }
            .animation(.easeInOut(duration: 0.3), value: isShowing)
    }
}

extension View {
    func toast(
        state: ToastState,
        isShowing: Bool,
        onDismiss: @escaping () -> Void
    ) -> some View {
        self.modifier(ToastModifier(
            toastState: state,
            isShowing: isShowing,
            onDismiss: onDismiss
        ))
    }
}
