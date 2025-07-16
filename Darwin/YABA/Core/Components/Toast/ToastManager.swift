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
    
    // Private property to track the auto-hide task
    private var autoHideTask: Task<Void, Never>?
    
    func show(
        message: LocalizedStringKey,
        contentColor: Color? = nil,
        accentColor: Color? = nil,
        acceptText: LocalizedStringKey? = nil,
        iconType: ToastIconType = .none,
        duration: ToastDuration = .short,
        position: ToastPosition = .bottom,
        onAcceptPressed: (() -> Void)? = nil
    ) {
        Task {
            // Cancel any existing auto-hide task
            autoHideTask?.cancel()
            
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
            
            // Start auto-hide timer
            startAutoHideTimer(duration: duration)
        }
    }
    
    func hide() {
        Task {
            // Cancel any pending auto-hide task
            autoHideTask?.cancel()
            autoHideTask = nil
            
            if isShowing {
                self.isShowing = false
                try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
            }
            
            self.toastState = ToastState()
        }
    }
    
    // Private method to handle auto-hide timer
    private func startAutoHideTimer(duration: ToastDuration) {
        autoHideTask = Task {
            do {
                try await Task.sleep(nanoseconds: duration.getDuration())
                // Only hide if the task wasn't cancelled and toast is still showing
                if !Task.isCancelled && isShowing {
                    hide()
                }
            } catch {
                // Task was cancelled, do nothing
            }
        }
    }
}
