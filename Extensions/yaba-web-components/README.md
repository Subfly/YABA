# YABA Web Components

WebView-hosted components for YABA: TipTap editor, read-only viewer, and HTML-to-reader-HTML converter. Built with Vite 7, React 19, TipTap 3.20, and TypeScript.

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
| `converter.html` | Hidden utility page: DOMPurify → Readability → sanitized reader HTML |

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
| `setDocumentJson(documentJson, options?)` | Set content from TipTap/ProseMirror JSON string; `options.assetsBaseUrl` resolves `../assets/` paths in the JSON |
| `getDocumentJson()` | Returns current document as JSON string |
| `focus()` / `blur()` | Focus/blur editor |
| `dispatch(cmd)` | Run command; see below |

### Commands (`dispatch`)

- `{ type: 'toggleBold' }`, `toggleItalic`, `toggleUnderline`, `toggleStrikethrough`, `toggleCode`
- `{ type: 'toggleQuote' }`, `{ type: 'insertHr' }`
- `{ type: 'toggleBulletedList' }`, `{ type: 'toggleNumberedList' }`
- `{ type: 'indent' }`, `{ type: 'outdent' }`
- `{ type: 'undo' }`, `{ type: 'redo' }`
- `{ type: 'insertLink', url: string }`, `{ type: 'removeLink' }`

### `window.YabaConverterBridge` (converter.html)

| Method | Description |
|--------|-------------|
| `sanitizeAndConvertHtmlToReaderHtml(input)` | `input: { html: string, baseUrl?: string }` → `{ documentJson: string, assets: [...] }` |

Uses DOMPurify for sanitization, Mozilla Readability for reader-mode extraction (strips nav/footer/clutter), then rewrites image URLs to `yaba-asset://` placeholders, parses with TipTap, and returns canonical document JSON plus asset descriptors for offline download.

## Native Integration

Native platforms call the bridge via their WebView evaluation APIs:

- **Android**: `webView.evaluateJavascript("window.YabaEditorBridge?.getDocumentJson()", callback)`
- **iOS**: `webView.evaluateJavaScript("window.YabaEditorBridge?.getDocumentJson()", completionHandler)`

The `converter.html` page is loaded in a hidden WebView when link saving needs extraction. Call `sanitizeAndConvertHtmlToReaderHtml` after the page has loaded; native persists `documentJson` (not HTML).

## Features

- **Images**: Inline images in HTML/JSON; `setDocumentJson` accepts `assetsBaseUrl` to resolve `../assets/` paths. Android WebView uses `allowFileAccess` for `file://` image URLs.
- **Tables, task lists, code**: Native TipTap document model
- **Code highlighting**: Syntax highlighting via lowlight
- **Mathematics**: LaTeX math via KaTeX; inline `$...$` and block `$$...$$`; `dispatch({ type: 'insertInlineMath', latex: '...' })` / `{ type: 'insertBlockMath', latex: '...' }`
- **Subscript / Superscript**: `dispatch({ type: 'toggleSubscript' })` / `{ type: 'toggleSuperscript' }`

## Follow-ups (Not in Scope)

- Native WebView wrappers and asset packaging
