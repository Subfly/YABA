import type { IHighlight, LTWHP, Scaled, ScaledPosition } from "react-pdf-highlighter"
import {
  createRangeByOffsets,
  parsePageIndex,
  type PdfHighlightForRendering,
} from "./pdf-text-utils"

/** PDF.js viewer page view (minimal shape used for highlight conversion). */
export interface PdfPageViewLike {
  div: HTMLElement
  viewport: { width: number; height: number }
  textLayer?: { div: HTMLElement }
}

export interface PdfViewerLike {
  getPageView: (index: number) => PdfPageViewLike | null | undefined
}

function viewportToScaled(rect: LTWHP, viewport: { width: number; height: number }): Scaled {
  return {
    x1: rect.left,
    y1: rect.top,
    x2: rect.left + rect.width,
    y2: rect.top + rect.height,
    width: viewport.width,
    height: viewport.height,
    pageNumber: rect.pageNumber,
  }
}

function getBoundingRectOnPage(rects: LTWHP[], pageNumber: number): LTWHP {
  const onPage = rects.filter((r) => (r.pageNumber ?? pageNumber) === pageNumber)
  if (onPage.length === 0) {
    return { left: 0, top: 0, width: 0, height: 0, pageNumber }
  }
  let left = Number.POSITIVE_INFINITY
  let top = Number.POSITIVE_INFINITY
  let right = Number.NEGATIVE_INFINITY
  let bottom = Number.NEGATIVE_INFINITY
  for (const r of onPage) {
    left = Math.min(left, r.left)
    top = Math.min(top, r.top)
    right = Math.max(right, r.left + r.width)
    bottom = Math.max(bottom, r.top + r.height)
  }
  return {
    left,
    top,
    width: right - left,
    height: bottom - top,
    pageNumber,
  }
}

/**
 * Converts persisted YABA text-offset highlights into `react-pdf-highlighter` model.
 */
export function yabaHighlightsToLibraryHighlights(
  viewer: PdfViewerLike,
  highlights: PdfHighlightForRendering[],
): IHighlight[] {
  const out: IHighlight[] = []

  for (const h of highlights) {
    const startPage = parsePageIndex(h.startSectionKey)
    const endPage = parsePageIndex(h.endSectionKey)
    if (startPage === null || endPage === null) continue

    const viewportRects: LTWHP[] = []
    const pageRangeStart = Math.min(startPage, endPage)
    const pageRangeEnd = Math.max(startPage, endPage)

    for (let pageIndex = pageRangeStart; pageIndex <= pageRangeEnd; pageIndex += 1) {
      const pageView = viewer.getPageView(pageIndex)
      const textLayer = pageView?.textLayer?.div
      if (!pageView || !textLayer) continue

      const pageTextLength = textLayer.textContent?.length ?? 0
      const fromOffset = pageIndex === startPage ? h.startOffsetInSection : 0
      const toOffset = pageIndex === endPage ? h.endOffsetInSection : pageTextLength
      if (fromOffset >= toOffset) continue

      const range = createRangeByOffsets(textLayer, fromOffset, toOffset)
      if (!range) continue

      const pageDiv = pageView.div
      const pageNumber = pageIndex + 1
      const pageRect = pageDiv.getBoundingClientRect()
      const clientRects = Array.from(range.getClientRects())

      for (const cr of clientRects) {
        if (cr.width <= 0 || cr.height <= 0) continue
        if (!Number.isFinite(cr.left)) continue
        viewportRects.push({
          top: cr.top + pageDiv.scrollTop - pageRect.top,
          left: cr.left + pageDiv.scrollLeft - pageRect.left,
          width: cr.width,
          height: cr.height,
          pageNumber,
        })
      }
    }

    if (viewportRects.length === 0) continue

    const primaryPageNumber = Math.min(
      ...viewportRects.map((r) => r.pageNumber ?? 1),
    )

    const scaledRects: Scaled[] = viewportRects.map((r) => {
      const idx = (r.pageNumber ?? primaryPageNumber) - 1
      const pv = viewer.getPageView(idx)
      const vp = pv?.viewport ?? { width: 1, height: 1 }
      return viewportToScaled(r, vp)
    })

    const boundingViewport = getBoundingRectOnPage(viewportRects, primaryPageNumber)
    const pageViewForBounding = viewer.getPageView(primaryPageNumber - 1)
    const vport = pageViewForBounding?.viewport ?? { width: 1, height: 1 }
    const boundingScaled = viewportToScaled(boundingViewport, vport)

    const position: ScaledPosition = {
      pageNumber: primaryPageNumber,
      boundingRect: boundingScaled,
      rects: scaledRects,
    }

    out.push({
      id: h.id,
      position,
      content: { text: "" },
      comment: { text: "", emoji: "" },
    })
  }

  return out
}
