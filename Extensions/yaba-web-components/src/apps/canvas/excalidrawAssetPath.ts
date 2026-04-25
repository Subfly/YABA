/**
 * Excalidraw resolves font files as `./fonts/<Family>/...` against `window.EXCALIDRAW_ASSET_PATH`,
 * then falls back to the esm.sh bundle (blocked by YABA CSP).
 * This module must load before any `@excalidraw/excalidraw` import.
 *
 * Use the same directory as `canvas.html` (e.g. `…/WebComponents/`) so fonts live in `./fonts/`
 * next to the page — no separate `excalidraw-assets/` copy of `dist/prod/fonts` is required.
 */
if (typeof window !== "undefined") {
  window.EXCALIDRAW_ASSET_PATH = new URL(".", window.location.href).href
}
