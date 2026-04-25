# YABA Web Components

WebView-hosted bundles for YABA: **TipTap** note editor, **read-it-later** (static HTML reader with selection + annotations), **reader-converter** (Mozilla Readability → readable HTML), **Excalidraw** canvas, and **EPUB.js** reader. Built with Vite 7, React 19 (editor/canvas), and TypeScript.

## Build

```bash
npm install
npm run build
```

Output: `dist/editor.html`, `dist/read-it-later.html`, `dist/reader-converter.html`, `dist/canvas.html`, `dist/epub-viewer.html` plus JS/CSS assets. Run `npm run dev` for local development.

## Entrypoints

| File | Purpose |
|------|---------|
| `editor.html` | TipTap WYSIWYG note editor |
| `read-it-later.html` | Saved link reader: injects HTML into the DOM; selection + annotation bridge (no TipTap) |
| `reader-converter.html` | Hidden page: `Readability` extracts readable HTML from an HTML string |
| `canvas.html` | Excalidraw canvas |
| `epub-viewer.html` | EPUB reader (epub.js) |

## URL parameters

Loaded by the native WebView. These apply to shells that read the shared theme helpers (`editor.html`, `read-it-later.html`, `epub-viewer.html`, etc.).

| Param | Required | Values | Description |
|-------|----------|--------|-------------|
| `platform` | Yes | `compose` \| `darwin` | Theme palette and font stack (`compose` is treated as Android) |
| `appearance` | No | `light` \| `dark` | Override; otherwise `prefers-color-scheme` when `auto` |
| `cursor` | No | CSS color | Caret / cursor color (e.g. folder color) |

### Example

```
editor.html?platform=compose&cursor=%23FF7C75
read-it-later.html?platform=darwin&appearance=dark
```

## JS bridge API (native → WebView)

Hosts call these via `evaluateJavascript` / `evaluateJavaScript` on the loaded page.

### `window.YabaEditorBridge` (`editor.html`)

Same command surface as before: ProseMirror JSON, formatting, mentions, math, note autosave idle, optional PDF export (`html2pdf.js`), etc. See `src/bridge/editor-bridge.ts`.

### `window.YabaReadItLaterBridge` (`read-it-later.html`)

Passive HTML surface: `setHtml` / `setReaderHtml` load article HTML; `setAnnotations` applies highlight colors to `span.yaba-annotation-mark` regions; selection and `applyAnnotationToSelection` / `removeAnnotationFromDocument` operate on the live DOM. Typing mirrors the old read-only viewer subset of `YabaEditorBridge` where native still expects the same method names.

### `window.YabaReaderConverterBridge` (`reader-converter.html`)

| Method | Description |
|--------|-------------|
| `convertHtmlToReadableHtml(input)` | `input: { html: string, baseUrl?: string }` → `Promise<{ readableHtml: string, title: string \| null }>` |

No DOMPurify, metadata scraping, PDF/EPUB extraction jobs, or TipTap JSON in this bundle.

### `window.YabaEpubBridge` (`epub-viewer.html`)

Unchanged EPUB reader bridge. See `src/apps/epub-viewer/epub-viewer-bridge.ts`.

## Web → native (`window.YabaNativeHost.postMessage`)

Structured JSON envelopes are defined in `src/bridge/contracts/native-host.ts`, including `bridgeReady` (`feature`: `editor` \| `read-it-later` \| `reader-converter` \| `epub` \| `canvas`), `shellLoad`, `toc`, `readerMetrics`, and EPUB/Canvas-specific payloads.

**Images in read-it-later:** `http`/`https` and `data:` image URLs are not loaded; inline assets should use `../assets/…` with `assetsBaseUrl` like the editor, or `file:` paths from the host.

## Features (editor)

- Rich text, tables, task lists, code (lowlight), math (KaTeX)
- Exports: Markdown and PDF (editor only; `html2pdf.js`)

## Follow-ups

- Native app URL constants and script strings that still reference `viewer.html` / `converter.html` / `pdf-viewer.html` must be updated in a platform ticket to match the new `read-it-later.html` and `reader-converter.html` entrypoints and bridge global names.
