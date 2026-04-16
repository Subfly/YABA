//
//  PrivateBookmarkSessionGuard.swift
//  YABACore
//
//  Compose `PrivateBookmarkSessionGuard` parity: in-memory unlock until [lock] (app background).
//

import Foundation
import SwiftUI

@MainActor
@Observable
public final class PrivateBookmarkSessionGuard {
    public static let shared = PrivateBookmarkSessionGuard()

    public private(set) var isUnlocked: Bool = false

    private init() {}

    public var isLocked: Bool { !isUnlocked }

    public func unlock() {
        let wasLocked = !isUnlocked
        isUnlocked = true
        if wasLocked {
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Private Session Unlocked Toast"),
                iconType: .hint
            )
        }
    }

    public func lock() {
        let wasUnlocked = isUnlocked
        isUnlocked = false
        if wasUnlocked {
            CoreToastManager.shared.show(
                message: LocalizedStringKey("Private Session Locked Toast"),
                iconType: .hint
            )
        }
    }
}
