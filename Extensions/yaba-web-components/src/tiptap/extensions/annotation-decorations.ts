import { Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { Decoration, DecorationSet } from "@tiptap/pm/view"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import { YabaAnnotationMarkName } from "./yaba-annotation-mark"

export const ANNOTATIONS_META_KEY = "yaba-annotations"
export const annotationDecorationPluginKey = new PluginKey("annotationDecorations")

/** Native-driven annotation list: id + color role; positions come from `yabaAnnotation` marks in the document. */
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

function getAnnotationClass(colorRole: string): string {
  return colorRoleToClass[colorRole] ?? "yaba-highlight-yellow"
}

/** True if the selection range overlaps any persisted `yabaAnnotation` mark (for “can create” guard on viewer). */
export function selectionOverlapsYabaAnnotationMark(
  doc: ProseMirrorNode,
  from: number,
  to: number
): boolean {
  let overlaps = false
  doc.nodesBetween(from, to, (node) => {
    if (!node.isText) return true
    const has = node.marks.some((m) => m.type.name === YabaAnnotationMarkName)
    if (has) {
      overlaps = true
      return false
    }
    return true
  })
  return overlaps
}

function mapAnnotationsToDecorations(
  doc: ProseMirrorNode,
  annotations: AnnotationForRendering[]
): Decoration[] {
  const idToRole = new Map(annotations.map((h) => [h.id, h.colorRole]))
  const decorations: Decoration[] = []

  doc.descendants((node, pos) => {
    if (!node.isText) return
    const mark = node.marks.find((m) => m.type.name === YabaAnnotationMarkName)
    if (!mark) return
    const id = mark.attrs.id as string | undefined
    if (!id || !idToRole.has(id)) return
    const cls = getAnnotationClass(idToRole.get(id)!)
    const from = pos
    const to = pos + node.nodeSize
    decorations.push(
      Decoration.inline(from, to, {
        class: `yaba-annotation-decoration ${cls}`,
        "data-annotation-id": id,
      })
    )
  })
  return decorations
}

export const AnnotationDecorationsExtension = Extension.create({
  name: "annotationDecorations",

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: annotationDecorationPluginKey,
        state: {
          init(_, state) {
            const decos = mapAnnotationsToDecorations(state.doc, storedAnnotations)
            return DecorationSet.create(state.doc, decos)
          },
          apply(tr, pluginState) {
            const meta = tr.getMeta(ANNOTATIONS_META_KEY)
            if (meta !== undefined) {
              const decos = mapAnnotationsToDecorations(tr.doc, meta)
              return DecorationSet.create(tr.doc, decos)
            }
            if (tr.docChanged) {
              const decos = mapAnnotationsToDecorations(tr.doc, storedAnnotations)
              return DecorationSet.create(tr.doc, decos)
            }
            return pluginState
          },
        },
        props: {
          decorations(state) {
            return this.getState(state)
          },
          handleClick(_view, _pos, event) {
            const target = event.target as HTMLElement
            const el = target.closest?.(".yaba-annotation-decoration") as HTMLElement | null
            const annotationId = el?.getAttribute?.("data-annotation-id")
            if (annotationId) {
              event.preventDefault()
              const win = window as Window & {
                YabaEditorBridge?: { onAnnotationTap?: (id: string) => void }
              }
              win.YabaEditorBridge?.onAnnotationTap?.(annotationId)
              return true
            }
            return false
          },
        },
      }),
    ]
  },
})
