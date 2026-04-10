# Compose ↔ Darwin YABACore state parity matrix

This document maps each Compose `*Event.kt` under `Compose/YABA/app/src/main/java/dev/subfly/yaba/core/state/` to the Darwin implementation under `Darwin/YABACore/State/`.

**Conventions**

- Darwin uses **IDs and value types** where Compose passes rich UI models (`FolderUiModel`, etc.).
- **Reads** (lists, search rows) stay in SwiftUI `@Query`; state machines hold **UI-only** draft, filters, selection, and issue **mutations** via managers.
- **Contract-only** events (share/export/pickers/reminders) update `UIState` or are no-ops until the app wires platform code.

## Base / shared types

| Compose | Darwin |
|--------|--------|
| `FolderSelectionMode` | `YabaCoreFolderSelectionMode` in `YabaCoreStateTypes.swift` |
| `SortType` / `SortOrderType` | `YabaCoreSortType` / `YabaCoreSortOrderType` |
| `BookmarkAppearance` / `CardImageSizing` | `YabaCoreBookmarkAppearance` / `YabaCoreCardImageSizing` |
| `ReaderTheme` / `ReaderFontSize` / `ReaderLineHeight` | `YabaCoreReaderTheme` / `YabaCoreReaderFontSize` / `YabaCoreReaderLineHeight` |
| `YabaColor` | `YabaCoreColorRole` (raw `Int` / `code`) |
| `ReadableSelectionDraft` | `YabaReadableSelectionDraft` |

## Feature tables

### `home/HomeEvent.kt`

| Compose case | Darwin `HomeEvent` | Notes |
|--------------|-------------------|--------|
| `OnInit` | `onInit` | Sets `HomeUIState.isLoading` |
| Preference changes | `onChangeBookmarkAppearance` / `onChangeCardImageSizing` / `onChangeCollectionSorting` / `onChangeSortOrder` | UI-only; host may sync to `@AppStorage` |
| `OnToggleFolderExpanded` | `onToggleFolderExpanded` | Updates `expandedFolderIds` |
| `OnDeleteFolder` / `OnMoveFolder` | `onDeleteFolder` / `onMoveFolder` | `FolderManager` |
| `OnDeleteTag` | `onDeleteTag` | `TagManager` |
| Bookmark deletes/moves | `onDeleteBookmark` / `onMoveBookmarkToFolder` / `onMoveBookmarkToTag` | `AllBookmarksManager` |

### `search/SearchEvent.kt`

| Compose case | Darwin `SearchEvent` | Notes |
|--------------|---------------------|--------|
| `OnInit` | `onInit` | No-op (hook) |
| `OnChangeQuery` | `onChangeQuery` | |
| `OnToggleFolderFilter` / `OnToggleTagFilter` | `onToggleFolderFilter` / `onToggleTagFilter` | `SearchUIState` filter sets |
| `OnChangeSort` | `onChangeSort` | |
| `OnChangeAppearance` | `onChangeAppearance` | |
| `OnDeleteBookmark` | `onDeleteBookmark` | `AllBookmarksManager` |

### `selection/folder/FolderSelectionEvent.kt`

| Compose case | Darwin `FolderSelectionEvent` | Notes |
|--------------|------------------------------|--------|
| `OnInit` | `onInit` + `ensureUncategorizedExists` | Mode/context stored in `FolderSelectionUIState` |
| `OnSearchQueryChanged` | `onSearchQueryChanged` | |
| `OnMoveFolderToSelected` | `onMoveFolderToSelected` | `FolderManager.queueMoveFolder` |
| `OnMoveBookmarksToSelected` | `onMoveBookmarksToSelected` | `AllBookmarksManager.queueMoveBookmarksToFolder` |

### `selection/bookmark/BookmarkSelectionEvent.kt`

| Compose case | Darwin `BookmarkSelectionEvent` | Notes |
|--------------|--------------------------------|--------|
| `OnInit` / `OnChangeQuery` / `OnSelectBookmark` | same | |
| — | `delete` / `moveToFolder` | Bulk helpers (Darwin) |

### `selection/tag/TagSelectionEvent.kt`

| Compose case | Darwin `TagSelectionEvent` | Notes |
|--------------|---------------------------|--------|
| `OnInit` / `OnSearchQueryChanged` / `OnSelectTag` / `OnDeselectTag` | same | Tag IDs only |

### `selection/icon/*`

| Compose case | Darwin | Notes |
|----------------|--------|--------|
| `IconSelectionEvent` | `IconSelectionEvent` | `YabaIconCategory`; loads icons on `onInit` |
| `IconCategorySelectionEvent.OnInit` | `IconCategorySelectionEvent.onInit` | `IconManager.loadAllCategories` |

### `creation/bookmark` (product route)

Compose uses kind-specific `creation/linkmark` in core; Darwin exposes link entry at `Creation/Bookmark/` (`BookmarkCreationStateMachine`).

### `creation/notemark/NotemarkMentionCreationEvent.kt`

| Compose case | Darwin `NotemarkMentionCreationEvent` | Notes |
|--------------|--------------------------------------|--------|
| `OnInit` | `onInit` | |
| `OnChangeMentionText` | `onChangeMentionText` | |
| `OnBookmarkPickedFromSelection` | `onBookmarkPickedFromSelection` | Optional `bookmarkLabel` for auto-fill |

### Detail / Creation `*mark` and folder/tag/annotation

For each `Detail/<Kind>/` and `Creation/<Kind>/`, Darwin defines `*UIState`, `*Event`, and `*StateMachine` with Compose-aligned case names (Swift `camelCase`). Persistence uses existing managers (`AllBookmarksManager`, `LinkmarkManager`, `DocmarkManager`, `AnnotationManager`, `ReadableVersionManager`, `ReadableContentManager`, `CanvmarkManager`, `NotemarkManager`, `ImagemarkManager`, `FolderManager`, `TagManager`).

**Intentionally contract-only on Darwin** (no Core filesystem / picker / notification implementation yet): share/export lines, some `OnWebInitialContentLoad`, reminder scheduling, and pure host WebView bridge hooks — events exist so the app can connect platform behavior without reshaping the API again.

## Maintenance

When adding a Compose event case, update the matching Darwin `*Event.swift`, extend `*UIState` if it carries new draft state, implement `send(_:)` (or document as contract-only), and adjust this matrix.
