/**
 * Excalidraw resolves font files as `./fonts/...` against `window.EXCALIDRAW_ASSET_PATH`,
 * then falls back to `https://esm.sh/@excalidraw/excalidraw@…/dist/prod/` (blocked by YABA CSP).
 * This module must load before any `@excalidraw/excalidraw` import.
 *
 * `import.meta.env.BASE_URL` is `./` — combined with the page URL so assets work under
 * `…/web-components/canvas.html`, not only at origin root.
 */
if (typeof window !== "undefined") {
  const base = new URL(import.meta.env.BASE_URL, window.location.href)
  window.EXCALIDRAW_ASSET_PATH = new URL("excalidraw-assets/", base).href
}
