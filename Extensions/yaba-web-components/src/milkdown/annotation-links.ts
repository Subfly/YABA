import type { Node as ProseNode } from "@milkdown/prose/model"
import { ANNOTATION_HREF_PREFIX } from "./yaba-href"

export interface AnnotationForRendering {
  id: string
  colorRole: string
}

let storedAnnotations: AnnotationForRendering[] = []

export function setStoredAnnotations(annotations: AnnotationForRendering[]): void {
  storedAnnotations = annotations
}

export function getStoredAnnotations(): AnnotationForRendering[] {
  return storedAnnotations
}

/** Selection overlaps a yaba-annotation link (reader guard for “create annotation”). */
export function selectionOverlapsAnnotationLink(doc: ProseNode, from: number, to: number): boolean {
  let overlaps = false
  doc.nodesBetween(from, to, (node) => {
    if (!node.isText) return true
    for (const m of node.marks) {
      if (m.type.name === "link") {
        const href = (m.attrs as { href?: string }).href ?? ""
        if (href.startsWith(ANNOTATION_HREF_PREFIX)) {
          overlaps = true
          return false
        }
      }
    }
    return true
  })
  return overlaps
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

/** Remove markdown links pointing at yaba-annotation:<id>, leaving link text. */
export function stripAnnotationLinksForId(markdown: string, annotationId: string): string {
  const esc = escapeRegExp(annotationId)
  const re = new RegExp(`\\[([^\\]]+)\\]\\(${escapeRegExp(ANNOTATION_HREF_PREFIX)}${esc}\\)`, "g")
  return markdown.replace(re, "$1")
}

/** Apply native-driven color classes to annotation anchors in the live DOM. */
export function syncAnnotationAnchorDom(root: ParentNode | null | undefined): void {
  if (!root) return
  root.querySelectorAll<HTMLAnchorElement>('a[href^="yaba-annotation:"]').forEach((a) => {
    const href = a.getAttribute("href") ?? ""
    const id = href.replace(new RegExp(`^${escapeRegExp(ANNOTATION_HREF_PREFIX)}`), "").split(/[?#]/)[0]
    a.className = ""
    a.removeAttribute("data-annotation-id")
    const ann = storedAnnotations.find((x) => x.id === id)
    if (ann) {
      const role = (ann.colorRole || "YELLOW").toUpperCase()
      a.classList.add(`yaba-annotation--${role}`)
      a.setAttribute("data-annotation-id", id)
    }
  })
}
