//
//  PrivateBookmarkPasswordVerifier.swift
//  YABACore
//
//  Compose `PrivateBookmarkPasswordVerifier` parity: 6-digit PIN vs stored string (UTF-8),
//  constant-time comparison.
//

import Foundation

public enum PrivateBookmarkPasswordVerifier {
    /// Verifies the 6-digit private bookmark PIN against the value stored in app preferences.
    public static func verify(pinDigits: String, stored: String) -> Bool {
        if stored.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return false
        }
        let normalized = pinDigits.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalized.count == 6, normalized.allSatisfy(\.isNumber) else {
            return false
        }
        let left = Data(normalized.utf8)
        let right = Data(stored.utf8)
        return secureBytesEqual(left, right)
    }

    private static func secureBytesEqual(_ lhs: Data, _ rhs: Data) -> Bool {
        guard lhs.count == rhs.count else { return false }
        var diff: UInt8 = 0
        for idx in lhs.indices {
            diff |= lhs[idx] ^ rhs[idx]
        }
        return diff == 0
    }
}
