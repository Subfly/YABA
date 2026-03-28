import type { TocItemJson, TocJson } from "@/bridge/toc-host-events"

/** pdf.js document (minimal surface for outline). */
type PdfJsDoc = {
  getOutline: () => Promise<PdfOutlineItem[] | null>
  getDestination: (name: string) => Promise<unknown>
  getPageIndex: (ref: unknown) => Promise<number>
}

export type PdfOutlineItem = {
  title: string
  dest?: string | unknown[] | unknown
  items?: PdfOutlineItem[]
}

async function resolveOutlineItemToPageNumber(
  pdf: PdfJsDoc,
  item: PdfOutlineItem,
): Promise<number | null> {
  const dest = item.dest
  if (dest == null) return null

  try {
    if (typeof dest === "string") {
      const resolved = await pdf.getDestination(dest)
      if (resolved == null) return null
      const ref = Array.isArray(resolved) ? resolved[0] : resolved
      const idx = await pdf.getPageIndex(ref)
      return Number.isFinite(idx) ? idx + 1 : null
    }
    if (Array.isArray(dest)) {
      const ref = dest[0]
      const idx = await pdf.getPageIndex(ref)
      return Number.isFinite(idx) ? idx + 1 : null
    }
    const idx = await pdf.getPageIndex(dest)
    return Number.isFinite(idx) ? idx + 1 : null
  } catch {
    return null
  }
}

async function mapOutlineTree(
  pdf: PdfJsDoc,
  items: PdfOutlineItem[],
  depth: number,
  idCounter: { n: number },
): Promise<TocItemJson[]> {
  const out: TocItemJson[] = []
  for (const it of items) {
    const title = (it.title || "").trim()
    if (!title) continue
    const page = await resolveOutlineItemToPageNumber(pdf, it)
    const id = `pdf-toc-${idCounter.n++}`
    const level = Math.min(6, Math.max(1, depth))
    const extrasJson = page != null ? JSON.stringify({ page }) : null
    const children =
      it.items && it.items.length > 0
        ? await mapOutlineTree(pdf, it.items, depth + 1, idCounter)
        : []
    out.push({
      id,
      title,
      level,
      children,
      extrasJson,
    })
  }
  return out
}

/**
 * Builds a [TocJson] from pdf.js `getOutline()`, or `null` if no outline / empty.
 */
export async function buildPdfOutlineToc(pdfDocument: unknown): Promise<TocJson | null> {
  const pdf = pdfDocument as PdfJsDoc
  if (!pdf || typeof pdf.getOutline !== "function") return null
  const outline = await pdf.getOutline().catch(() => null)
  if (!outline || outline.length === 0) return null
  const items = await mapOutlineTree(pdf, outline as PdfOutlineItem[], 1, { n: 0 })
  if (items.length === 0) return null
  return { items }
}
