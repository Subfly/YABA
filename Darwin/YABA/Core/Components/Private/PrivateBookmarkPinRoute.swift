//
//  PrivateBookmarkPinRoute.swift
//  YABA
//
//  Sheet routing for private-bookmark PIN flows (creation + item actions).
//

import SwiftUI

enum PrivateBookmarkPinRoute: Identifiable, Equatable {
    case create
    case entry(bookmarkId: String?, reason: PrivateBookmarkPasswordReason)

    var id: String {
        switch self {
        case .create:
            "privatePin.create"
        case let .entry(bookmarkId, reason):
            "privatePin.entry.\(bookmarkId ?? "nil").\(reason.rawValue)"
        }
    }
}

@MainActor
enum PrivateBookmarkCreationPinGate {
    /// Compose `rememberPrivateBookmarkCreationToggle` parity.
    static func run(
        pinSheet: Binding<PrivateBookmarkPinRoute?>,
        onToggleAllowed: () async -> Void
    ) async {
        if !PrivateBookmarkPasswordStore.hasPin {
            pinSheet.wrappedValue = .create
            return
        }
        if PrivateBookmarkSessionGuard.shared.isLocked {
            pinSheet.wrappedValue = .entry(bookmarkId: nil, reason: .unlockSession)
            return
        }
        await onToggleAllowed()
    }
}
