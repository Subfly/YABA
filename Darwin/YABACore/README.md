# YABACore (Darwin)

SwiftData persistence layer aligned with the Compose Room schema: **folders**, **tags**, **bookmarks**, subtype details, **readable versions**, **annotations**, and **data-log** tombstones.

## Entry points

- **Schema:** `YabaSchemaV2` — `VersionedSchema` at version `2.0.0`.
- **Migration:** `YabaComposeParityMigrationPlan` — migrates released `YabaSchemaV1` stores (see `YabaV1ToV2Migrator`).
- **Container:** `YabaComposeParityModelContainer.makeContainer()` / `makeContext()`.

## Legacy mapping

See `Persistence/LegacyFieldMapping.md`.

## Next steps (out of scope for this package)

Managers, state machines, view-model wiring, and swapping `YabaModelContainer` in the app target are handled in follow-up work. See `Persistence/DEFERRED.md`.
