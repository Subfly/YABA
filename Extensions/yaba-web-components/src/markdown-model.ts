/**
 * Canonical markdown conventions for YABA web components.
 *
 * - **Notes (editor)**: GFM + Crepe/Milkdown features; text “highlights” use inline HTML
 *   `<mark class="yaba-editor-text-highlight yaba-highlight-{ROLE}" data-color-role="{ROLE}">…</mark>` when needed.
 * - **Read-it-later (viewer)**: Same markdown; **annotations** (native-managed, with ids) use inline HTML:
 *   `<span data-yaba-annotation-id="{id}" class="yaba-annotation-mark">…</span>`.
 *   Colors come from `setAnnotations([{ id, colorRole }])` which applies decoration classes in the shell.
 * - **Bookmark mentions**: `[label](bookmark:{bookmarkId})` with optional query-style metadata encoded in label or as `bookmark:{id}` href only.
 */

/** Prefix for bookmark-mention links in markdown. */
export const BOOKMARK_LINK_PREFIX = "bookmark:" as const

/** Regex to match markdown images for asset pruning. */
export const MARKDOWN_IMAGE_RE = /!\[[^\]]*]\(([^)]+)\)/g
