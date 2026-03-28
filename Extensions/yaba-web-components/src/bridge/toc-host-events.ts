/**
 * Table of contents updates for native WebView hosts.
 * Must match Core [YabaWebBridgeScripts.TOC_EVENT_PREFIX].
 */
export const YABA_TOC_EVENT_PREFIX = "yaba-toc:"

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
  console.info(`${YABA_TOC_EVENT_PREFIX}${json}`)
}

export function resetPublishedToc(): void {
  lastPublishedTocJson = null
}
