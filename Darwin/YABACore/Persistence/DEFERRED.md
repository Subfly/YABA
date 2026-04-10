# Deferred (app target / follow-up)

## Done in YABACore (this package)

- `YabaCoreStore` — cached `ModelContainer` + write `ModelContext` factory.
- `YabaCoreOperationQueue` — serialized persistence operations.
- Static managers (`AllBookmarksManager`, `FolderManager`, `TagManager`, `LinkmarkManager`, `AnnotationManager`, `ReadableVersionManager`) and request DTOs.
- Observable state machine shells (`BookmarkCreationStateMachine`, `HomeStateMachine`) and base types in `State/`.

## Still deferred — `Darwin/YABA` application target

- **Inject** compose-parity store: replace `YabaModelContainer.getContext()` / `YabaSchemaV1` with `YabaCoreStore` / v2 models in `YABAApp` and downstream views.
- **Rewire screens** — move legacy `*State.swift` flows to `@Query` + YABACore state machines / managers; drop obsolete Darwin-only behaviors removed on Compose (e.g. legacy drag-and-drop move semantics).
- **Compile fixes** across `Darwin/YABA` after switching `ModelContext` and model types.
- **Optional** repository shims if a thinner boundary is needed between SwiftUI and managers.
