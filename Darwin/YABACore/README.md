# YABACore (Darwin)

SwiftData persistence layer aligned with the Compose Room schema: **folders**, **tags**, **bookmarks**, subtype details, **readable versions**, and **annotations**.

## Entry points

- **Schema:** `YabaSchemaV2` — `VersionedSchema` at version `2.0.0`.
- **Migration:** `YabaComposeParityMigrationPlan` — migrates released `YabaSchemaV1` stores (see `YabaV1ToV2Migrator`).
- **Container / contexts:** `YabaCoreStore.sharedContainer()` caches the default `ModelContainer`; `YabaCoreStore.makeWriteContext()` returns a fresh `ModelContext` (autosave off). `YabaComposeParityModelContainer` delegates to `YabaCoreStore` for API compatibility.

## Managers & queue

- **Serial writes:** `YabaCoreOperationQueue` — FIFO mutations with explicit `save()` boundaries (Compose `CoreOperationQueue` analogue).
- **Domains (Compose parity):**  
  `AllBookmarksManager`, `FolderManager`, `TagManager`, `LinkmarkManager`, `ImagemarkManager`, `DocmarkManager`, `NotemarkManager`, `CanvmarkManager`, `AnnotationManager`, `ReadableVersionManager`, `ReadableContentManager`, `IconManager`, `GlobalDataManager` — static entry points; **Compose** remains the behavioral source of truth for filling in edge cases.

## State machines

- **Pattern:** Screen-owned observable types in `State/` hold **UI-only** state; lists and models should be read with SwiftUI `@Query` and passed into `send(_:)` as parameters where needed.
- **Primitives:** `State/Base/YabaBaseObservableState.swift`, `State/Base/YabaScreenStateMachine.swift`.
- **Layout:** One folder per feature area. Each machine is split into **`FeatureUIState`**, **`FeatureEvent`**, and **`FeatureStateMachine`** (e.g. `State/Home/`, `State/Search/`, `State/Creation/Bookmark/` for the shared link-creation route).
- **Compose ↔ Darwin map:** `State/PARITY_MATRIX.md` lists feature-level parity and conventions.
- **Compose-parity groups:** `State/Detail/<Kind>/` and `State/Creation/<Kind>/` for link/doc/image/note/canvas flows; `NotemarkMention` types live beside `NotemarkCreation` under `State/Creation/Notemark/`; `State/Detail/Folder`, `State/Detail/Tag`, `State/Creation/Folder`, `State/Creation/Tag`, `State/Creation/Annotation`; `State/Selection/<Kind>/` for multi-select and icon-picker flows. Shared model enums: `State/YabaCoreStateTypes.swift`.

## Legacy mapping

See `Persistence/LegacyFieldMapping.md`.

## App integration

Wiring `Darwin/YABA` to this layer (environment `modelContext`, replacing legacy `YabaSchemaV1`, etc.) is tracked in `Persistence/DEFERRED.md`.
