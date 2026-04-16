//
//  PrivateBookmarkPasswordStore.swift
//  YABACore
//
//  Stores the app-wide private-bookmark PIN string under the same settings key as Compose
//  (`Constants.SettingsKeys.privateBookmarkPasswordHash`). Value is plaintext digits, matching Android.
//

import Foundation

public enum PrivateBookmarkPasswordStore {
    private static var defaults: UserDefaults { .standard }

    public static func storedPin() -> String {
        defaults.string(forKey: Constants.SettingsKeys.privateBookmarkPasswordHash) ?? ""
    }

    public static func setStoredPin(_ value: String) {
        defaults.set(value, forKey: Constants.SettingsKeys.privateBookmarkPasswordHash)
    }

    public static func clearStoredPin() {
        defaults.removeObject(forKey: Constants.SettingsKeys.privateBookmarkPasswordHash)
    }

    public static var hasPin: Bool {
        !storedPin().trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
