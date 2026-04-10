# Legacy Darwin (YabaSchemaV1) → Compose-parity SwiftData (YabaSchemaV2)

This document freezes the field mapping used by `YabaComposeParityMigration`.

## Collection → FolderModel

| Legacy `Collection` | `FolderModel` |
|---------------------|---------------|
| Only when `type == folder` | row created |
| `collectionId` | `folderId` — if legacy id is `"-1"` (uncategorized), migrated to Compose stable id `11111111-1111-1111-1111-111111111111` (`Constants.Folder.Uncategorized.id`) |
| `label` | `label` |
| — | `folderDescription` = `nil` (Compose has optional description; legacy had none) |
| `icon` | `icon` |
| `color` (YabaColor raw) | `colorRaw` |
| `createdAt` | `createdAt` |
| `editedAt` | `editedAt` |
| — | `isHidden` = false |
| `parent` / `children` | same tree via relationships |

Legacy `Collection.order` is **not** migrated; Compose `FolderEntity` has no custom-order field.

## Collection → TagModel

| Legacy `Collection` | `TagModel` |
|---------------------|------------|
| Only when `type == tag` | row created |
| `collectionId` | `tagId` |
| `label` | `label` |
| `icon` | `icon` |
| `color` (raw) | `colorRaw` |
| `createdAt` | `createdAt` |
| `editedAt` | `editedAt` |
| — | `isHidden` = false |

Tag parent/child hierarchy from legacy is not preserved (noise per plan).

## Bookmark → BookmarkModel + LinkBookmarkModel

All released Darwin bookmarks are treated as **linkmarks** only.

| Legacy `Bookmark` | `BookmarkModel` |
|-------------------|-----------------|
| `bookmarkId` | `bookmarkId` |
| `label` | `label` |
| `bookmarkDescription` | `bookmarkDescription` |
| `createdAt` | `createdAt` |
| `editedAt` | `editedAt` |
| — | `kindRaw` = `0` (LINK, matches Compose `BookmarkKind`) |
| — | `viewCount` = 0 |
| — | `isPrivate` = false |
| — | `isPinned` = false |

| Legacy `Bookmark` | `LinkBookmarkModel` |
|---------------------|---------------------|
| `link` | `url` |
| `domain` | `domain` |
| `videoUrl` | `videoUrl` |
| — | `audioUrl` = nil |
| `label` | `metadataTitle` (same as bookmark title for now) |
| `bookmarkDescription` | `metadataDescription` (same as bookmark description for now) |
| — | `metadataAuthor` = nil |
| — | `metadataDate` = nil |

## Bookmark image/icon Data

| Legacy | New |
|--------|-----|
| `imageDataHolder` | `BookmarkImagePayloadModel.bytes` (relationship from `BookmarkModel`) |
| `iconDataHolder` | `BookmarkIconPayloadModel.bytes` |

## Folder membership

- **Owning folder:** first legacy `Collection` in `bookmark.collections` with `collectionType == .folder`, else uncategorized system folder.
- **Tags:** every legacy `Collection` with `collectionType == .tag` linked via `BookmarkModel.tags` ↔ `TagModel.bookmarks`.

## DataLog

Migrated rows copied to `DataLogEntryModel` (same logical fields, explicit `logId`).

## Not migrated from legacy bookmark into other subtypes

No `ImageBookmarkModel`, `NoteBookmarkModel`, `DocBookmarkModel`, or `CanvasBookmarkModel` rows are created from legacy data.
