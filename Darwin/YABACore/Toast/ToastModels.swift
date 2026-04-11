//
//  ToastModels.swift
//  YABACore
//
//  Parity with Compose `ToastModels.kt` — identifiers, durations, icons, per-toast visibility.
//

import SwiftUI

public typealias ToastId = String

/// Display duration for a toast (matches Compose `ToastDuration` millisecond values).
public enum ToastDuration: Sendable {
    case short
    case long

    public var nanoseconds: UInt64 {
        switch self {
        case .short:
            4_000_000_000
        case .long:
            8_000_000_000
        }
    }
}

/// Icon token for the toast surface (asset names align with Compose `ToastIconType`).
public enum ToastIconType: Sendable, Equatable {
    case warning
    case success
    case hint
    case error
    case none

    /// Bundle / design-system image name for Darwin toast UI.
    public var iconAssetName: String {
        switch self {
        case .warning:
            "alert-02"
        case .success:
            "checkmark-badge-02"
        case .hint:
            "information-circle"
        case .error:
            "cancel-circle"
        case .none:
            "help-circle"
        }
    }
}

/// One toast entry; `isVisible` drives exit animation before removal (Compose parity).
public struct ToastItem: Identifiable {
    public let id: ToastId
    public var message: LocalizedStringKey
    public var acceptText: LocalizedStringKey?
    public var iconType: ToastIconType
    public var duration: ToastDuration
    public var isVisible: Bool

    public init(
        id: ToastId,
        message: LocalizedStringKey,
        acceptText: LocalizedStringKey? = nil,
        iconType: ToastIconType = .none,
        duration: ToastDuration = .short,
        isVisible: Bool = true
    ) {
        self.id = id
        self.message = message
        self.acceptText = acceptText
        self.iconType = iconType
        self.duration = duration
        self.isVisible = isVisible
    }

    func withVisible(_ visible: Bool) -> ToastItem {
        var copy = self
        copy.isVisible = visible
        return copy
    }
}
