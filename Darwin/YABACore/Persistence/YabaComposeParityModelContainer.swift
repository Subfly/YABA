//
//  YabaComposeParityModelContainer.swift
//  YABACore
//
//  Preferred entry for opening the on-disk store with V1→V2 migration.
//
//  Cached container access and write contexts: ``YabaCoreStore``.
//

import Foundation
import SwiftData

public enum YabaComposeParityModelContainer {
    /// Builds a `ModelContainer` for the compose-parity schema, migrating from `YabaSchemaV1` when needed.
    /// Delegates to ``YabaCoreStore/sharedContainer(isStoredInMemoryOnly:)`` so callers share one store instance.
    public static func makeContainer(isStoredInMemoryOnly: Bool = false) throws -> ModelContainer {
        try YabaCoreStore.sharedContainer(isStoredInMemoryOnly: isStoredInMemoryOnly)
    }

    /// Convenience `ModelContext` with autosave disabled (matches legacy `YabaModelContainer` default).
    public static func makeContext(isStoredInMemoryOnly: Bool = false) throws -> ModelContext {
        try YabaCoreStore.makeWriteContext(isStoredInMemoryOnly: isStoredInMemoryOnly)
    }
}
