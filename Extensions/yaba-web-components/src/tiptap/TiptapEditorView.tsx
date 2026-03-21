import { useEffect, useRef, useCallback } from "react"
import { EditorProvider, useCurrentEditor } from "@tiptap/react"
import { createEditorExtensions } from "./editor-extensions"
import "./tiptap-styles.css"

const EMPTY_DOC = { type: "doc" as const, content: [] }

const IMAGE_NOT_FOUND_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none">
  <path d="M15.5 8C15.7761 8 16 7.77614 16 7.5C16 7.22386 15.7761 7 15.5 7M15.5 8C15.2239 8 15 7.77614 15 7.5C15 7.22386 15.2239 7 15.5 7M15.5 8V7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M2 2L22 22" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M17.2997 21.2997C16.0187 21.5 14.3303 21.5 12 21.5C7.77027 21.5 5.6554 21.5 4.25276 20.302C4.05358 20.1319 3.86808 19.9464 3.69797 19.7472C2.5 18.3446 2.5 16.2297 2.5 12C2.5 9.66971 2.5 7.98134 2.70033 6.70033M20.0355 20.0355C20.1281 19.943 20.217 19.8468 20.302 19.7472C21.5 18.3446 21.5 16.2297 21.5 12C21.5 7.77027 21.5 5.6554 20.302 4.25276C20.1319 4.05358 19.9464 3.86808 19.7472 3.69797C18.3446 2.5 16.2297 2.5 12 2.5C7.77027 2.5 5.6554 2.5 4.25276 3.69797C4.15317 3.78303 4.057 3.87193 3.96447 3.96447" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M3 16L7.50036 11.5004M21 16L18.5303 13.5303C18.1908 13.1908 17.7302 13 17.25 13C16.7698 13 16.3092 13.1908 15.9697 13.5303L14.75 14.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`

interface TiptapEditorViewProps {
  editable: boolean
  /** TipTap/ProseMirror JSON document as string. */
  initialDocumentJson?: string
  onEditorReady?: (editor: import("@tiptap/core").Editor) => void
  assetsBaseUrl?: string
}

function rewriteAssetPathsInDocumentJson(json: string, assetsBaseUrl: string): string {
  if (!json.includes("../assets/")) return json
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return json.replaceAll("../assets/", `${base}assets/`)
}

function parseInitialContent(initialDocumentJson: string | undefined, assetsBaseUrl: string | undefined): Record<string, unknown> {
  let raw = initialDocumentJson?.trim() ? initialDocumentJson : ""
  if (assetsBaseUrl && raw.includes("../assets/")) {
    raw = rewriteAssetPathsInDocumentJson(raw, assetsBaseUrl)
  }
  if (!raw) return EMPTY_DOC as Record<string, unknown>
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return EMPTY_DOC as Record<string, unknown>
  }
}

function handleBrokenImage(img: HTMLImageElement) {
  if (img.dataset.yabaFallback) return
  img.dataset.yabaFallback = "true"

  const wrapper = document.createElement("div")
  wrapper.className = "yaba-image-not-available"

  const iconWrapper = document.createElement("div")
  iconWrapper.className = "yaba-image-not-available-icon"
  iconWrapper.innerHTML = IMAGE_NOT_FOUND_SVG

  const label = document.createElement("span")
  label.className = "yaba-image-not-available-label"
  label.textContent = "Image not available"

  wrapper.appendChild(iconWrapper)
  wrapper.appendChild(label)
  img.parentNode?.replaceChild(wrapper, img)
}

function BrokenImageObserver() {
  const containerRef = useRef<HTMLDivElement | null>(null)

  const attachErrorListeners = useCallback((root: HTMLElement) => {
    root.querySelectorAll<HTMLImageElement>("img:not([data-yaba-fallback])").forEach((img) => {
      if (img.complete && img.naturalWidth === 0 && img.src) handleBrokenImage(img)
      else img.addEventListener("error", () => handleBrokenImage(img), { once: true })
    })
  }, [])

  useEffect(() => {
    const container = containerRef.current?.closest("[data-yaba-editor-root]") as HTMLElement | null
    if (!container) return

    attachErrorListeners(container)

    const observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        m.addedNodes.forEach((node) => {
          if (node instanceof HTMLImageElement) {
            if (node.complete && node.naturalWidth === 0 && node.src) handleBrokenImage(node)
            else node.addEventListener("error", () => handleBrokenImage(node), { once: true })
          } else if (node instanceof HTMLElement) {
            attachErrorListeners(node)
          }
        })
      }
    })
    observer.observe(container, { childList: true, subtree: true })
    return () => observer.disconnect()
  }, [attachErrorListeners])

  return <div ref={containerRef} style={{ display: "none" }} />
}

function EditorReadyNotifier({
  onEditorReady,
}: {
  onEditorReady?: (editor: import("@tiptap/core").Editor) => void
}) {
  const { editor } = useCurrentEditor()

  useEffect(() => {
    if (editor && onEditorReady) {
      onEditorReady(editor)
    }
  }, [editor, onEditorReady])

  return null
}

export function TiptapEditorView({
  editable,
  initialDocumentJson,
  onEditorReady,
  assetsBaseUrl,
}: TiptapEditorViewProps) {
  const content = parseInitialContent(initialDocumentJson, assetsBaseUrl)

  const extensions = createEditorExtensions()

  return (
    <div className="yaba-editor-container" data-yaba-editor-root>
      <EditorProvider
        extensions={extensions}
        editable={editable}
        content={content}
        editorProps={{
          attributes: {
            class: "yaba-content-editable",
            style: "caret-color: var(--yaba-cursor, var(--yaba-primary)); outline: none;",
          },
          handleDOMEvents: {
            error: (_, err) => {
              console.error("TipTap error:", err)
            },
          },
        }}
        slotBefore={
          <>
            <EditorReadyNotifier onEditorReady={onEditorReady} />
            <BrokenImageObserver />
          </>
        }
      />
    </div>
  )
}
