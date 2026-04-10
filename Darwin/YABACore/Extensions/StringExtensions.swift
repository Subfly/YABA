//
//  StringExtensions.swift
//  YABACore
//

import Foundation

public extension String {
    var nilIfEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
