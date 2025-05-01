//
//  ToastManager.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//


import SwiftUI

@MainActor
@Observable
class ToastManager {
    var toastState: ToastState = .init()
    var isShowing = false
    
    func show(
        message: LocalizedStringKey,
        contentColor: Color? = nil,
        accentColor: Color? = nil,
        acceptText: String? = nil,
        iconType: ToastIconType = .none,
        duration: ToastDuration = .short,
        position: ToastPosition = .bottom,
        onAcceptPressed: (() -> Void)? = nil
    ) {
        Task {
            if self.isShowing {
                self.isShowing = false
                try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
            }
            
            self.toastState.message = message
            self.toastState.contentColor = contentColor
            self.toastState.accentColor = accentColor
            self.toastState.acceptText = acceptText
            self.toastState.iconType = iconType
            self.toastState.duration = duration
            self.toastState.position = position
            self.toastState.onAcceptPressed = onAcceptPressed
            
            self.isShowing = true
        }
    }
    
    func hide() {
        Task {
            if isShowing {
                self.isShowing = false
                try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
            }
            
            self.toastState = ToastState()
        }
    }
}
