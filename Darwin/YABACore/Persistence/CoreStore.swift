//
//  CoreStore.swift
//  YABACore
//
//  Centralizes compose-parity `ModelContainer` creation and write-context conventions.
//  Does not require a singleton `ModelContext`: callers may create contexts from the shared
//  container as needed; serialized writes use `CoreOperationQueue` + explicit `save()`.
//

import Foundation
import SwiftData

/// Shared access to the on-disk compose-parity store.
public enum CoreStore {
    private static let lock = NSLock()
    private static var cachedContainer: ModelContainer?

    /// Lazily creates and caches one `ModelContainer` per process for the default store URL.
    /// SwiftUI screens typically use `@Environment(\.modelContext)`; managers performing writes
    /// through `CoreOperationQueue` should use `makeWriteContext()` from the same container.
    public static func sharedContainer(isStoredInMemoryOnly: Bool = false) throws -> ModelContainer {
        lock.lock()
        defer { lock.unlock() }
        if let cachedContainer {
            return cachedContainer
        }
        let container = try makeFreshContainer(isStoredInMemoryOnly: isStoredInMemoryOnly)
        cachedContainer = container
        return container
    }

    /// One-off container build (same as historical `ParityModelContainer` implementation).
    private static func makeFreshContainer(isStoredInMemoryOnly: Bool) throws -> ModelContainer {
        let schema = Schema(versionedSchema: SchemaV2.self)
        let config = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: isStoredInMemoryOnly
        )
        return try ModelContainer(
            for: schema,
            migrationPlan: ParityMigrationPlan.self,
            configurations: [config]
        )
    }

    /// Fresh `ModelContext` with autosave disabled (matches legacy Darwin defaults).
    public static func makeWriteContext(isStoredInMemoryOnly: Bool = false) throws -> ModelContext {
        let container = try sharedContainer(isStoredInMemoryOnly: isStoredInMemoryOnly)
        let context = ModelContext(container)
        context.autosaveEnabled = false
        return context
    }

    /// Persists pending changes; call after mutations when using explicit-save mode.
    public static func save(_ context: ModelContext) throws {
        if context.hasChanges {
            try context.save()
        }
    }
}
