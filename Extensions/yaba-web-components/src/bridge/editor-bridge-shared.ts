/**
 * Minimal shared state for the editor/viewer bridge (Crepe instance, view access, assets URL, cursor).
 */
import type { Crepe } from "@milkdown/crepe"
import { editorViewCtx } from "@milkdown/core"
import type { EditorView } from "@milkdown/prose/view"

export let crepeInstance: Crepe | null = null

export function setCrepeInstance(c: Crepe | null): void {
  crepeInstance = c
}

/** Set when [setDocumentJson] / [setReaderHtml] runs with options; used to resolve inline image src and normalize saves. */
export let lastAssetsBaseUrl: string | undefined

export function setLastAssetsBaseUrl(url: string | undefined): void {
  lastAssetsBaseUrl = url
}

export function getLastAssetsBaseUrl(): string | undefined {
  return lastAssetsBaseUrl
}

export function getBridgeView(): EditorView | null {
  const c = crepeInstance
  if (!c) return null
  try {
    return c.editor.action((ctx) => ctx.get(editorViewCtx))
  } catch {
    return null
  }
}

let lastStoredCursor: { anchor: number; head: number } | null = null

export function getLastStoredCursor(): { anchor: number; head: number } | null {
  return lastStoredCursor
}

export function setLastStoredCursor(sel: { anchor: number; head: number } | null): void {
  lastStoredCursor = sel
}

export let systemColorSchemeMedia: MediaQueryList | null = null
export let systemColorSchemeListener: (() => void) | null = null

export function clearSystemColorSchemeListener(): void {
  if (!systemColorSchemeMedia || !systemColorSchemeListener) return

  systemColorSchemeMedia.removeEventListener("change", systemColorSchemeListener)
  systemColorSchemeListener = null
  systemColorSchemeMedia = null
}

export function ensureSystemColorSchemeListener(onChange: () => void): void {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return
  if (systemColorSchemeListener) return

  systemColorSchemeMedia = window.matchMedia("(prefers-color-scheme: dark)")
  systemColorSchemeListener = onChange
  systemColorSchemeMedia.addEventListener("change", onChange)
}
