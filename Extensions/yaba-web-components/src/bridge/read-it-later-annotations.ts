import type { AnnotationForRendering } from "./read-it-later-types"

const colorRoleToClass: Record<string, string> = {
  NONE: "yaba-highlight-yellow",
  BLUE: "yaba-highlight-blue",
  BROWN: "yaba-highlight-brown",
  CYAN: "yaba-highlight-cyan",
  GRAY: "yaba-highlight-gray",
  GREEN: "yaba-highlight-green",
  INDIGO: "yaba-highlight-indigo",
  MINT: "yaba-highlight-mint",
  ORANGE: "yaba-highlight-orange",
  PINK: "yaba-highlight-pink",
  PURPLE: "yaba-highlight-purple",
  RED: "yaba-highlight-red",
  TEAL: "yaba-highlight-teal",
  YELLOW: "yaba-highlight-yellow",
}

function getHighlightClass(colorRole: string): string {
  return colorRoleToClass[colorRole] ?? "yaba-highlight-yellow"
}

const ANNOTATION_MARK_SELECTOR = "span.yaba-annotation-mark[data-yaba-annotation-id]"

/** True if the range visually overlaps an existing yabaAnnotation mark (cannot nest). */
export function domSelectionOverlapsAnnotation(contentRoot: HTMLElement, range: Range): boolean {
  const spans = contentRoot.querySelectorAll(ANNOTATION_MARK_SELECTOR)
  for (const sp of Array.from(spans)) {
    const nr = document.createRange()
    try {
      nr.selectNodeContents(sp)
    } catch {
      continue
    }
    if (range.compareBoundaryPoints(Range.END_TO_START, nr) < 0) continue
    if (range.compareBoundaryPoints(Range.START_TO_END, nr) > 0) continue
    return true
  }
  return false
}

/**
 * Best-effort wrap of the current selection in an annotation mark span.
 */
export function wrapSelectionInAnnotation(
  range: Range,
  annotationId: string,
  contentRoot: HTMLElement,
): boolean {
  if (range.collapsed) return false
  if (domSelectionOverlapsAnnotation(contentRoot, range)) return false
  if (!contentRoot.contains(range.commonAncestorContainer)) return false

  const doc = contentRoot.ownerDocument
  const span = doc.createElement("span")
  span.className = "yaba-annotation-mark yaba-annotation-decoration"
  span.setAttribute("data-yaba-annotation-id", annotationId)
  span.setAttribute("data-annotation-id", annotationId)

  try {
    range.surroundContents(span)
  } catch {
    const contents = range.extractContents()
    span.appendChild(contents)
    range.insertNode(span)
  }
  return true
}

export function unwrapAnnotationMarks(contentRoot: HTMLElement, annotationId: string): number {
  const escaped =
    typeof CSS !== "undefined" && typeof CSS.escape === "function"
      ? CSS.escape(annotationId)
      : annotationId.replace(/["\\]/g, "\\$&")
  const sel = `span.yaba-annotation-mark[data-yaba-annotation-id="${escaped}"]`
  const nodes = contentRoot.querySelectorAll(sel)
  let count = 0
  nodes.forEach((node) => {
    const el = node as HTMLElement
    const parent = el.parentNode
    if (!parent) return
    while (el.firstChild) {
      parent.insertBefore(el.firstChild, el)
    }
    parent.removeChild(el)
    count += 1
  })
  return count
}

/** Apply color decoration classes from native annotation list. */
export function applyAnnotationColorDecorations(
  contentRoot: HTMLElement,
  annotations: AnnotationForRendering[],
): void {
  const idToRole = new Map(annotations.map((a) => [a.id, a.colorRole]))
  const highlightValues = new Set(Object.values(colorRoleToClass))
  const spans = contentRoot.querySelectorAll(ANNOTATION_MARK_SELECTOR) as NodeListOf<HTMLElement>
  spans.forEach((span) => {
    const id = span.getAttribute("data-yaba-annotation-id")
    if (!id) return
    const role = idToRole.get(id)
    if (!role) return
    const highlight = getHighlightClass(role)
    for (const c of highlightValues) {
      if (c !== highlight) {
        span.classList.remove(c)
      }
    }
    if (!span.classList.contains("yaba-annotation-decoration")) {
      span.classList.add("yaba-annotation-decoration")
    }
    span.classList.add(highlight)
  })
}
