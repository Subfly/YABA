import { defineConfig, type Plugin } from "vite"
import react from "@vitejs/plugin-react"
import { resolve } from "path"

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
  plugins: [react(), reactPdfHighlighterSinglePage()],
  base: "./",
  build: {
    outDir: "dist",
    rollupOptions: {
      input: {
        editor: resolve(__dirname, "editor.html"),
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
