//
//  ParityMigrationPlan.swift
//  YABACore
//

import SwiftData

/// Migrates released stores from `SchemaV1` to compose-parity `SchemaV2`.
enum ParityMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [SchemaV1.self, SchemaV2.self]
    }

    static var stages: [MigrationStage] {
        [migrateV1toV2]
    }

    static let migrateV1toV2 = MigrationStage.custom(
        fromVersion: SchemaV1.self,
        toVersion: SchemaV2.self,
        willMigrate: { context in
            try V1ToV2Migrator.run(context: context)
        },
        didMigrate: { _ in }
    )
}
