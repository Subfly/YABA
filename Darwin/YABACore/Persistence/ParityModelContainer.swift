//
//  ParityModelContainer.swift
//  YABACore
//
//  Preferred entry for opening the on-disk store with V1→V2 migration.
//
//  Cached container access and write contexts: ``CoreStore``.
//

import Foundation
import SwiftData

public enum ParityModelContainer {
    /// Builds a `ModelContainer` for the compose-parity schema, migrating from `SchemaV1` when needed.
    /// Delegates to ``CoreStore/sharedContainer(isStoredInMemoryOnly:)`` so callers share one store instance.
    public static func makeContainer(isStoredInMemoryOnly: Bool = false) throws -> ModelContainer {
        try CoreStore.sharedContainer(isStoredInMemoryOnly: isStoredInMemoryOnly)
    }

    /// Convenience `ModelContext` with autosave disabled (matches legacy `YabaModelContainer` default).
    public static func makeContext(isStoredInMemoryOnly: Bool = false) throws -> ModelContext {
        try CoreStore.makeWriteContext(isStoredInMemoryOnly: isStoredInMemoryOnly)
    }
}
