import { useEffect, useRef, useCallback } from "react"
import { Crepe, CrepeFeature } from "@milkdown/crepe"
import { Milkdown, MilkdownProvider, useEditor, useInstance } from "@milkdown/react"
import { getNoteEditorPlaceholderText } from "./note-placeholder"
import "@milkdown/crepe/theme/frame.css"
import "./yaba-crepe.css"

const IMAGE_NOT_FOUND_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" fill="none">
  <path d="M15.5 8C15.7761 8 16 7.77614 16 7.5C16 7.22386 15.7761 7 15.5 7M15.5 8C15.2239 8 15 7.77614 15 7.5C15 7.22386 15.2239 7 15.5 7M15.5 8V7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M2 2L22 22" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M17.2997 21.2997C16.0187 21.5 14.3303 21.5 12 21.5C7.77027 21.5 5.6554 21.5 4.25276 20.302C4.05358 20.1319 3.86808 19.9464 3.69797 19.7472C2.5 18.3446 2.5 16.2297 2.5 12C2.5 9.66971 2.5 7.98134 2.70033 6.70033M20.0355 20.0355C20.1281 19.943 20.217 19.8468 20.302 19.7472C21.5 18.3446 21.5 16.2297 21.5 12C21.5 7.77027 21.5 5.6554 20.302 4.25276C20.1319 4.05358 19.9464 3.86808 19.7472 3.69797C18.3446 2.5 16.2297 2.5 12 2.5C7.77027 2.5 5.6554 2.5 4.25276 3.69797C4.15317 3.78303 4.057 3.87193 3.96447 3.96447" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M3 16L7.50036 11.5004M21 16L18.5303 13.5303C18.1908 13.1908 17.7302 13 17.25 13C16.7698 13 16.3092 13.1908 15.9697 13.5303L14.75 14.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`

export type CrepeShellVariant = "viewer" | "editor"

export interface CrepeEditorViewProps {
  editable: boolean
  variant?: CrepeShellVariant
  /** Initial markdown document. */
  initialMarkdown?: string
  onCrepeReady?: (crepe: Crepe) => void
  assetsBaseUrl?: string
}

function rewriteAssetPathsInMarkdown(md: string, assetsBaseUrl: string): string {
  if (!md.includes("../assets/")) return md
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return md.replaceAll("../assets/", `${base}assets/`)
}

function parseInitialMarkdown(initialMarkdown: string | undefined, assetsBaseUrl: string | undefined): string {
  let raw = initialMarkdown?.trim() ? initialMarkdown : ""
  if (assetsBaseUrl && raw.includes("../assets/")) {
    raw = rewriteAssetPathsInMarkdown(raw, assetsBaseUrl)
  }
  return raw
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

function CrepeInner({
  editable,
  variant,
  initialMarkdown,
  onCrepeReady,
}: Omit<CrepeEditorViewProps, "assetsBaseUrl">) {
  const crepeRef = useRef<Crepe | null>(null)
  const readyRef = useRef(false)
  const defaultMd = parseInitialMarkdown(initialMarkdown, undefined)

  useEditor(
    (root: HTMLElement) => {
      const ph =
        variant === "editor"
          ? getNoteEditorPlaceholderText() || " "
          : " "
      const crepe = new Crepe({
        root,
        defaultValue: defaultMd || "",
        features: {
          [CrepeFeature.Toolbar]: false,
          [CrepeFeature.TopBar]: false,
        },
        featureConfigs: {
          [CrepeFeature.Placeholder]: {
            text: ph,
            mode: "block",
          },
        },
      })
      if (!editable) {
        crepe.setReadonly(true)
      }
      crepeRef.current = crepe
      return crepe
    },
    [editable, variant]
  )

  const [loading] = useInstance()

  useEffect(() => {
    if (loading || readyRef.current) return
    const c = crepeRef.current
    if (!c) return
    readyRef.current = true
    onCrepeReady?.(c)
  }, [loading, onCrepeReady])

  return (
    <>
      <Milkdown />
      <BrokenImageObserver />
    </>
  )
}

export function CrepeEditorView({
  editable,
  variant = "viewer",
  initialMarkdown,
  onCrepeReady,
  assetsBaseUrl,
}: CrepeEditorViewProps) {
  const md = parseInitialMarkdown(initialMarkdown, assetsBaseUrl)

  return (
    <div className="yaba-editor-container" data-yaba-editor-root>
      <MilkdownProvider>
        <CrepeInner
          editable={editable}
          variant={variant}
          initialMarkdown={md}
          onCrepeReady={onCrepeReady}
        />
      </MilkdownProvider>
    </div>
  )
}
