//
//  YabaCoreToastManager.swift
//  YABACore
//
//  Global toast orchestration (Compose `ToastManager.kt` parity): max 3 visible, FIFO queue,
//  staged dismiss animation, per-toast auto-hide. UI should observe only `visibleToasts`.
//

import Foundation
import Observation
import SwiftUI

/// Singleton toast manager — call from Core or app code without `EnvironmentObject` plumbing.
/// Named `YabaCoreToastManager` to avoid clashing with the app target’s UI `ToastManager`.
@MainActor
@Observable
public final class YabaCoreToastManager {
    public static let shared = YabaCoreToastManager()

    private static let maxVisibleToastCount = 3

    private struct PendingToast {
        var toast: ToastItem
        var onAcceptPressed: (() -> Void)?
    }

    /// Toasts currently rendered (or animating out). Host UI should bind to this only.
    public private(set) var visibleToasts: [ToastItem] = []

    private var queuedToasts: [PendingToast] = []
    private var onAcceptCallbacks: [ToastId: () -> Void] = [:]
    private var autoHideTasks: [ToastId: Task<Void, Never>] = [:]

    private init() {}

    // MARK: - Public API

    @discardableResult
    public func show(
        message: LocalizedStringKey,
        acceptText: LocalizedStringKey? = nil,
        iconType: ToastIconType = .none,
        duration: ToastDuration = .short,
        onAcceptPressed: (() -> Void)? = nil
    ) -> ToastId {
        let id = UUID().uuidString
        let toast = ToastItem(
            id: id,
            message: message,
            acceptText: acceptText,
            iconType: iconType,
            duration: duration,
            isVisible: true
        )

        if visibleToasts.count < Self.maxVisibleToastCount {
            visibleToasts.append(toast)
            if let onAcceptPressed {
                onAcceptCallbacks[id] = onAcceptPressed
            }
            startAutoHideJob(id: id, duration: duration)
        } else {
            queuedToasts.append(PendingToast(toast: toast, onAcceptPressed: onAcceptPressed))
        }

        return id
    }

    public func dismiss(id: ToastId) {
        if removeQueuedToastIfNeeded(id: id) {
            onAcceptCallbacks[id] = nil
            return
        }

        guard let visibleIndex = visibleToasts.firstIndex(where: { $0.id == id }) else { return }
        var target = visibleToasts[visibleIndex]
        if !target.isVisible { return }

        cancelAutoHide(id: id)
        target.isVisible = false
        visibleToasts[visibleIndex] = target

        Task { @MainActor in
            try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
            self.removeVisibleToastAndPromoteNext(id: id)
        }
    }

    public func dismissAll() {
        cancelAllAutoHideJobs()
        queuedToasts.removeAll()
        onAcceptCallbacks.removeAll()

        guard !visibleToasts.isEmpty else { return }

        visibleToasts = visibleToasts.map { $0.withVisible(false) }

        Task { @MainActor in
            try? await Task.sleep(nanoseconds: Constants.toastAnimationDuration)
            self.visibleToasts = []
        }
    }

    public func accept(id: ToastId) {
        let callback = onAcceptCallbacks[id]
        callback?()
        dismiss(id: id)
    }

    // MARK: - Private

    private func removeVisibleToastAndPromoteNext(id: ToastId) {
        visibleToasts.removeAll { $0.id == id }
        cancelAutoHide(id: id)
        onAcceptCallbacks[id] = nil

        while visibleToasts.count < Self.maxVisibleToastCount, !queuedToasts.isEmpty {
            var pending = queuedToasts.removeFirst()
            pending.toast.isVisible = true
            let promoted = pending.toast
            visibleToasts.append(promoted)
            if let onAccept = pending.onAcceptPressed {
                onAcceptCallbacks[promoted.id] = onAccept
            }
            startAutoHideJob(id: promoted.id, duration: promoted.duration)
        }
    }

    private func removeQueuedToastIfNeeded(id: ToastId) -> Bool {
        guard let index = queuedToasts.firstIndex(where: { $0.toast.id == id }) else {
            return false
        }
        queuedToasts.remove(at: index)
        return true
    }

    private func startAutoHideJob(id: ToastId, duration: ToastDuration) {
        cancelAutoHide(id: id)
        autoHideTasks[id] = Task { @MainActor in
            try? await Task.sleep(nanoseconds: duration.nanoseconds)
            guard !Task.isCancelled else { return }
            self.dismiss(id: id)
        }
    }

    private func cancelAutoHide(id: ToastId) {
        autoHideTasks[id]?.cancel()
        autoHideTasks[id] = nil
    }

    private func cancelAllAutoHideJobs() {
        for task in autoHideTasks.values {
            task.cancel()
        }
        autoHideTasks.removeAll()
    }
}
