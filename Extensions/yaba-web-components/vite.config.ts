import { existsSync, rmSync } from "node:fs"
import { fileURLToPath } from "node:url"
import { defineConfig, type Plugin } from "vite"
import react from "@vitejs/plugin-react"
import { resolve } from "path"

const __dirname = fileURLToPath(new URL(".", import.meta.url))

/**
 * Older builds copied Excalidraw fonts into `public/excalidraw-assets/`; Vite copies all of `public/`
 * into `dist/`. Remove any stale tree so it cannot reappear in output (fonts live in `public/fonts/`).
 */
function removeStaleExcalidrawAssetsCopy(): Plugin {
  const paths = [
    resolve(__dirname, "public/excalidraw-assets"),
    resolve(__dirname, "dist/excalidraw-assets"),
  ]
  return {
    name: "remove-stale-excalidraw-assets",
    buildStart() {
      for (const p of paths) {
        if (existsSync(p)) {
          rmSync(p, { recursive: true, force: true })
        }
      }
    },
  }
}

export default defineConfig({
  plugins: [react(), removeStaleExcalidrawAssetsCopy()],
  base: "./",
  build: {
    outDir: "dist",
    rollupOptions: {
      input: {
        editor: resolve(__dirname, "editor.html"),
        canvas: resolve(__dirname, "canvas.html"),
        "read-it-later": resolve(__dirname, "read-it-later.html"),
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
