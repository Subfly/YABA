# YABA Web Components

WebView-hosted components for YABA: **Milkdown Crepe** WYSIWYG editor, read-only viewer, and HTML→**Markdown** converter. Built with Vite 7, React 19, Milkdown 7.20, and TypeScript.

## Build

```bash
npm install
npm run build
```

Output: `dist/editor.html`, `dist/viewer.html`, `dist/converter.html` plus JS and CSS assets. Run `npm run dev` for local development.

## Entrypoints

| File | Purpose |
|------|---------|
| `editor.html` | Crepe/Milkdown WYSIWYG editor (no visible toolbar; native buttons only) |
| `viewer.html` | Read-only Milkdown viewer for saved link content |
| `converter.html` | Hidden utility page: DOMPurify → Readability → Showdown Markdown |

## URL Parameters

Loaded by the native WebView. All apply to `editor.html` and `viewer.html` unless noted.

| Param | Required | Values | Description |
|-------|----------|--------|-------------|
| `platform` | Yes | `compose` \| `darwin` | Determines theme palette and font stack |
| `appearance` | No | `light` \| `dark` | Override; otherwise uses `prefers-color-scheme` |
| `cursor` | No | CSS color (hex/rgb/hsl) | Cursor/caret color (e.g. folder color) |

### Platform Themes

- **compose**: Material 3 palette from YABA Compose app; Quicksand font (bundled)
- **darwin**: CSS system colors (`color-scheme`); system/SF font stack

### Example

```
editor.html?platform=compose&cursor=%23FF7C75
viewer.html?platform=darwin&appearance=dark
```

## JS Bridge API (v1)

### `window.YabaEditorBridge` (editor.html, viewer.html)

| Method | Description |
|--------|-------------|
| `isReady()` | Returns `true` when editor is mounted |
| `setPlatform(platform)` | `'android'` \| `'darwin'` (legacy: `'compose'` is accepted as android) |
| `setAppearance(mode)` | `'auto'` \| `'light'` \| `'dark'` |
| `setCursorColor(color)` | CSS color string |
| `setEditable(isEditable)` | Toggle edit/read-only |
| `setDocumentJson(markdown, options?)` | Set content from a **Markdown** string (legacy method name); `options.assetsBaseUrl` resolves `../assets/` paths for display |
| `getDocumentJson()` | Returns current document as **Markdown** string (legacy name) |
| `setReaderHtml(html, options?)` | One-shot load: HTML is sanitized and converted to Markdown |
| `focus()` / `unFocus()` | Focus/blur editor |
| `dispatch(cmd)` | Run command; see below |

### Commands (`dispatch`)

- `{ type: 'toggleBold' }`, `toggleItalic`, `toggleUnderline`, `toggleStrikethrough`, `toggleCode`
- `{ type: 'toggleQuote' }`, `{ type: 'insertHr' }`
- `{ type: 'toggleBulletedList' }`, `{ type: 'toggleNumberedList' }`, `{ type: 'toggleTaskList' }`
- `{ type: 'indent' }`, `{ type: 'outdent' }`
- `{ type: 'undo' }`, `{ type: 'redo' }`
- `insertLink`, `updateLink`, `removeLink`, `insertMention`, … (see `editor-command-payload.ts`)
- **Subscript / superscript** commands are accepted but intentionally no-op (removed from product surface).

### `window.YabaConverterBridge` (converter.html)

| Method | Description |
|--------|-------------|
| `sanitizeAndConvertHtmlToReaderHtml(input)` | `input: { html: string, baseUrl?: string }` → `{ markdown, documentJson, assets, linkMetadata }` |

`documentJson` is deprecated and mirrors `markdown` for older hosts. Prefer `markdown`.

Uses DOMPurify for sanitization, Mozilla Readability for reader-mode extraction (strips nav/footer/clutter), rewrites image URLs to `yaba-asset://` placeholders, converts article HTML to **Markdown** with Showdown, and returns asset descriptors for offline download.

## Native Integration

Native platforms call the bridge via their WebView evaluation APIs:

- **Android**: `webView.evaluateJavascript("window.YabaEditorBridge?.getDocumentJson()", callback)`
- **iOS**: `webView.evaluateJavaScript("window.YabaEditorBridge?.getDocumentJson()", completionHandler)`

### Web → native events (Android)

Host apps inject `window.YabaNativeHost.postMessage(jsonString)` (Android can also expose `window.YabaAndroidHost` as an alias; see `src/bridge/yaba-native-host.ts`). The web layer emits structured JSON for shell load, ToC, editor/reader metrics, `bridgeReady`, taps (annotation, math, inline link/mention), autosave idle, and converter job completion—**not** via `console.info` or custom URL schemes.

The `converter.html` page is loaded in a hidden WebView when link saving needs extraction. Call `sanitizeAndConvertHtmlToReaderHtml` after the page has loaded; native persists **Markdown** (`markdown` field).

## Features

- **Images**: Inline images; `setDocumentJson` accepts `assetsBaseUrl` to resolve `../assets/` paths. Android WebView uses `allowFileAccess` for `file://` image URLs.
- **Tables, task lists, code**: GFM-oriented Milkdown document model
- **Mathematics**: LaTeX via Crepe/KaTeX; inline `$...$` and block `$$...$$`; `dispatch({ type: 'insertInlineMath', latex: '...' })` / `{ type: 'insertBlockMath', latex: '...' }`
- **Reader annotations**: `yaba-annotation:<id>` links in Markdown (viewer); **highlights** in the note editor use separate formatting (not the annotation pipeline).

## Follow-ups (Not in Scope)

- Native WebView wrappers and asset packaging
