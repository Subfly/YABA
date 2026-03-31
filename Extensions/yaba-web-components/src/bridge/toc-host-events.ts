/**
 * Table of contents updates for native WebView hosts.
 */
import { postToYabaNativeHost } from "./yaba-native-host"

export interface TocHostPayload {
  type: "toc"
  toc: TocJson | null
}

export interface TocJson {
  items: TocItemJson[]
}

export interface TocItemJson {
  id: string
  title: string
  level: number
  children: TocItemJson[]
  extrasJson?: string | null
}

let lastPublishedTocJson: string | null = null

export function publishToc(toc: TocJson | null): void {
  const payload: TocHostPayload = { type: "toc", toc }
  const json = JSON.stringify(payload)
  if (json === lastPublishedTocJson) return
  lastPublishedTocJson = json
  postToYabaNativeHost({ type: "toc", toc })
}

export function resetPublishedToc(): void {
  lastPublishedTocJson = null
}
