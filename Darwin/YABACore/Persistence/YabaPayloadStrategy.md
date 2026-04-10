# Darwin payload strategy (SwiftData)

Compose stores several assets as **filesystem path strings**. On Darwin, large binary and document bodies are modeled as:

1. **Dedicated `@Model` payload types** (`BookmarkImagePayloadModel`, `DocBookmarkPayloadModel`, …) related from the parent entity.
2. **`@Attribute(.externalStorage)`** on `Data?` inside those payload models so SwiftData can keep the main SQLite store small.

## Rules

- Do **not** put large `Data` blobs directly on `BookmarkModel` / subtype rows; access via `bookmark.imagePayload?.bytes`, `docDetail?.payload?.bytes`, etc.
- Do **not** use `#Predicate` on externally stored `Data` fields (SwiftData limitation).
- Prefer **explicit relationship prefetch** (`relationshipKeyPathsForPrefetching` on `FetchDescriptor`) when loading lists that will immediately open payloads.

## Mapping from Compose paths

| Compose | Darwin (this layer) |
|--------|---------------------|
| `localImagePath` / image files | `BookmarkImagePayloadModel.bytes` |
| `localIconPath` / icon files | `BookmarkIconPayloadModel.bytes` |
| Note / canvas / readable JSON on disk | `NoteBookmarkPayloadModel.documentBody`, `CanvasBookmarkPayloadModel.sceneData`, `ReadableVersionPayloadModel.documentJson` |
