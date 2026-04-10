//
//  YabaComposeParityModelContainer.swift
//  YABACore
//
//  Preferred entry for opening the on-disk store with V1→V2 migration.
//

import Foundation
import SwiftData

public enum YabaComposeParityModelContainer {
    /// Builds a `ModelContainer` for the compose-parity schema, migrating from `YabaSchemaV1` when needed.
    public static func makeContainer(isStoredInMemoryOnly: Bool = false) throws -> ModelContainer {
        let schema = Schema(versionedSchema: YabaSchemaV2.self)
        let config = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: isStoredInMemoryOnly
        )
        return try ModelContainer(
            for: schema,
            migrationPlan: YabaComposeParityMigrationPlan.self,
            configurations: [config]
        )
    }

    /// Convenience `ModelContext` with autosave disabled (matches legacy `YabaModelContainer` default).
    public static func makeContext(isStoredInMemoryOnly: Bool = false) throws -> ModelContext {
        let container = try makeContainer(isStoredInMemoryOnly: isStoredInMemoryOnly)
        let context = ModelContext(container)
        context.autosaveEnabled = false
        return context
    }
}
