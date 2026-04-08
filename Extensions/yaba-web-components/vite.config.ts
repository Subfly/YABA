import { cpSync, existsSync, mkdirSync } from "node:fs"
import { fileURLToPath } from "node:url"
import { defineConfig, type Plugin } from "vite"
import react from "@vitejs/plugin-react"
import { resolve } from "path"

const __dirname = fileURLToPath(new URL(".", import.meta.url))

/**
 * Ship Excalidraw's `dist/prod/fonts` next to the canvas bundle so `EXCALIDRAW_ASSET_PATH`
 * can resolve `./fonts/...` on `'self'` (WebView CSP blocks the esm.sh fallback).
 */
function copyExcalidrawProdFonts(): Plugin {
  const src = resolve(__dirname, "node_modules/@excalidraw/excalidraw/dist/prod/fonts")
  const dest = resolve(__dirname, "public/excalidraw-assets/fonts")
  return {
    name: "copy-excalidraw-prod-fonts",
    buildStart() {
      if (!existsSync(src)) {
        this.warn(`[copy-excalidraw-prod-fonts] missing ${src} (run npm install)`)
        return
      }
      mkdirSync(resolve(__dirname, "public/excalidraw-assets"), { recursive: true })
      cpSync(src, dest, { recursive: true })
    },
  }
}

/**
 * `react-pdf-highlighter` always constructs `PDFViewer` (all pages in DOM).
 * Swap to `PDFSinglePageViewer` so only the current page is mounted — required for large PDFs.
 */
function reactPdfHighlighterSinglePage(): Plugin {
  return {
    name: "react-pdf-highlighter-single-page",
    transform(code, id) {
      const normalized = id.replace(/\\/g, "/")
      if (!normalized.includes("/react-pdf-highlighter/")) return null
      if (!normalized.includes("PdfHighlighter")) return null
      if (!code.includes("new pdfjs.PDFViewer(")) return null
      return {
        code: code.replaceAll("new pdfjs.PDFViewer(", "new pdfjs.PDFSinglePageViewer("),
        map: null,
      }
    },
  }
}

export default defineConfig({
  plugins: [react(), reactPdfHighlighterSinglePage(), copyExcalidrawProdFonts()],
  base: "./",
  build: {
    outDir: "dist",
    rollupOptions: {
      input: {
        editor: resolve(__dirname, "editor.html"),
        canvas: resolve(__dirname, "canvas.html"),
        viewer: resolve(__dirname, "viewer.html"),
        converter: resolve(__dirname, "converter.html"),
        "pdf-viewer": resolve(__dirname, "pdf-viewer.html"),
        "epub-viewer": resolve(__dirname, "epub-viewer.html"),
      },
      output: {
        entryFileNames: "[name].js",
        chunkFileNames: "chunks/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash][extname]",
      },
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
})
