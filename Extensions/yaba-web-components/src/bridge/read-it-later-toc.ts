import { publishToc, resetPublishedToc, type TocJson, type TocItemJson } from "./toc-host-events"

const TOC_DEBOUNCE_MS = 1000
let tocTimer: ReturnType<typeof setTimeout> | null = null

function clearTocTimer(): void {
  if (tocTimer !== null) {
    clearTimeout(tocTimer)
    tocTimer = null
  }
}

function buildHeadingTocFromDom(root: HTMLElement): TocJson | null {
  const headingEls = Array.from(
    root.querySelectorAll<HTMLElement>("h1, h2, h3, h4, h5, h6"),
  ).filter((el) => (el.textContent || "").trim().length > 0)
  if (headingEls.length === 0) return null

  type Item = {
    id: string
    title: string
    level: number
    children: Item[]
    extrasJson?: string | null
  }
  const rootItems: Item[] = []
  const stack: { level: number; children: Item[] }[] = [{ level: 0, children: rootItems }]

  let idCounter = 0
  for (const h of headingEls) {
    const level = Math.min(6, Math.max(1, parseInt(h.tagName.slice(1), 10) || 1))
    const id = `toc-h-${idCounter++}`
    const text = h.textContent?.trim() ?? ""
    if (!text) continue
    const extrasJson = JSON.stringify({ tag: h.tagName.toLowerCase() })
    const item: Item = { id, title: text, level, children: [], extrasJson }
    while (stack.length > 1 && stack[stack.length - 1].level >= level) {
      stack.pop()
    }
    stack[stack.length - 1].children.push(item)
    stack.push({ level, children: item.children })
  }

  const mapItem = (x: Item): TocItemJson => ({
    id: x.id,
    title: x.title,
    level: x.level,
    children: x.children.map(mapItem),
    extrasJson: x.extrasJson,
  })
  return { items: rootItems.map(mapItem) }
}

export function scheduleReadItLaterTocPublish(contentRoot: HTMLElement | null): void {
  if (!contentRoot) {
    return
  }
  clearTocTimer()
  tocTimer = setTimeout(() => {
    tocTimer = null
    const toc = buildHeadingTocFromDom(contentRoot)
    publishToc(toc)
  }, TOC_DEBOUNCE_MS)
}

export function resetReadItLaterTocState(): void {
  clearTocTimer()
  resetPublishedToc()
}

export { buildHeadingTocFromDom }
