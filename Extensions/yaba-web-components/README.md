# YABA Web Components

WebView-hosted components for YABA: TipTap editor, read-only viewer, and HTML-to-Markdown converter. Built with Vite 7, React 19, TipTap 3.20, Excalidraw 0.18, and TypeScript.

## Build

```bash
npm install
npm run build
```

Output: `dist/editor.html`, `dist/viewer.html`, `dist/converter.html` plus JS and CSS assets. Run `npm run dev` for local development.

## Entrypoints

| File | Purpose |
|------|---------|
| `editor.html` | Full TipTap WYSIWYG editor (no visible toolbar; native buttons only) |
| `viewer.html` | Read-only TipTap viewer for saved link content |
| `converter.html` | Hidden utility page: DOMPurify → Readability → Turndown (HTML→Markdown) |

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
| `setPlatform(platform)` | `'compose'` \| `'darwin'` |
| `setAppearance(mode)` | `'auto'` \| `'light'` \| `'dark'` |
| `setCursorColor(color)` | CSS color string |
| `setEditable(isEditable)` | Toggle edit/read-only |
| `setMarkdown(markdown, options?)` | Set content; `options.assetsBaseUrl` resolves `../assets/` image paths |
| `getMarkdown()` | Returns current markdown string |
| `focus()` / `blur()` | Focus/blur editor |
| `dispatch(cmd)` | Run command; see below |

### Commands (`dispatch`)

- `{ type: 'toggleBold' }`, `toggleItalic`, `toggleUnderline`, `toggleStrikethrough`, `toggleCode`
- `{ type: 'toggleQuote' }`, `{ type: 'insertHr' }`
- `{ type: 'toggleBulletedList' }`, `{ type: 'toggleNumberedList' }`
- `{ type: 'indent' }`, `{ type: 'outdent' }`
- `{ type: 'undo' }`, `{ type: 'redo' }`
- `{ type: 'insertLink', url: string }`, `{ type: 'removeLink' }`
- `{ type: 'openExcalidraw' }` — Opens Excalidraw modal; on Save, inserts an Excalidraw node into the editor

### `window.YabaConverterBridge` (converter.html)

| Method | Description |
|--------|-------------|
| `sanitizeAndConvertHtmlToMarkdown(input)` | `input: { html: string, baseUrl?: string }` → `{ markdown: string }` |

Uses DOMPurify for sanitization, Mozilla Readability for reader-mode extraction (strips nav/footer/clutter), then Turndown + GFM for conversion (tables, strikethrough, etc.).

## Native Integration

Native platforms call the bridge via their WebView evaluation APIs:

- **Android**: `webView.evaluateJavascript("window.YabaEditorBridge?.getMarkdown()", callback)`
- **iOS**: `webView.evaluateJavaScript("window.YabaEditorBridge?.getMarkdown()", completionHandler)`

The `converter.html` page is loaded in a hidden WebView when link saving needs HTML→Markdown conversion. Call `sanitizeAndConvertHtmlToMarkdown` after the page has loaded.

## Features

- **Images**: Markdown image syntax (`![](url)`); `setMarkdown` accepts `assetsBaseUrl` to resolve `../assets/` paths. Android WebView uses `allowFileAccess` for `file://` image URLs.
- **GFM tables**: Markdown import and export for tables
- **Task lists**: GFM task list syntax (`- [ ]` / `- [x]`)
- **Code highlighting**: Syntax highlighting via lowlight
- **Mathematics**: LaTeX math via KaTeX; inline `$...$` and block `$$...$$`; `dispatch({ type: 'insertInlineMath', latex: '...' })` / `{ type: 'insertBlockMath', latex: '...' }`
- **Subscript / Superscript**: `dispatch({ type: 'toggleSubscript' })` / `{ type: 'toggleSuperscript' }`
- **YouTube**: `dispatch({ type: 'insertYouTube', url: 'https://youtube.com/watch?v=...' })`; paste of YouTube URLs auto-embeds
- **Excalidraw**: Insert diagrams via `dispatch({ type: 'openExcalidraw' })`; Excalidraw nodes serialize as fenced blocks (` ```yaba-excalidraw `) and render as SVG; Edit button to modify existing diagrams

## Follow-ups (Not in Scope)

- Native WebView wrappers and asset packaging
