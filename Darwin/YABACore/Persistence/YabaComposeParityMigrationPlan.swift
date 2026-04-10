//
//  YabaComposeParityMigrationPlan.swift
//  YABACore
//

import SwiftData

/// Migrates released stores from `YabaSchemaV1` to compose-parity `YabaSchemaV2`.
enum YabaComposeParityMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [YabaSchemaV1.self, YabaSchemaV2.self]
    }

    static var stages: [MigrationStage] {
        [migrateV1toV2]
    }

    static let migrateV1toV2 = MigrationStage.custom(
        fromVersion: YabaSchemaV1.self,
        toVersion: YabaSchemaV2.self,
        willMigrate: { context in
            try YabaV1ToV2Migrator.run(context: context)
        },
        didMigrate: { _ in }
    )
}
