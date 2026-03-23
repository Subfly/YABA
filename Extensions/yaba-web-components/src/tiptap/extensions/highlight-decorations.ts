import { Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { Decoration, DecorationSet } from "@tiptap/pm/view"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import { YabaHighlightMarkName } from "./yaba-highlight-mark"

export const HIGHLIGHTS_META_KEY = "yaba-highlights"
export const highlightDecorationPluginKey = new PluginKey("highlightDecorations")

/** Native-driven highlight list: id + color role; positions come from `yabaHighlight` marks in the document. */
export interface HighlightForRendering {
  id: string
  colorRole: string
}

let storedHighlights: HighlightForRendering[] = []

export function setStoredHighlights(highlights: HighlightForRendering[]): void {
  storedHighlights = highlights
}

export function getStoredHighlights(): HighlightForRendering[] {
  return storedHighlights
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

function getHighlightClass(colorRole: string): string {
  return colorRoleToClass[colorRole] ?? "yaba-highlight-yellow"
}

/** True if the selection range overlaps any `yabaHighlight` mark (for “can create” guard). */
export function selectionOverlapsYabaHighlightMark(
  doc: ProseMirrorNode,
  from: number,
  to: number
): boolean {
  let overlaps = false
  doc.nodesBetween(from, to, (node) => {
    if (!node.isText) return true
    const has = node.marks.some((m) => m.type.name === YabaHighlightMarkName)
    if (has) {
      overlaps = true
      return false
    }
    return true
  })
  return overlaps
}

function mapHighlightsToDecorations(
  doc: ProseMirrorNode,
  highlights: HighlightForRendering[]
): Decoration[] {
  const idToRole = new Map(highlights.map((h) => [h.id, h.colorRole]))
  const decorations: Decoration[] = []

  doc.descendants((node, pos) => {
    if (!node.isText) return
    const mark = node.marks.find(m => m.type.name === YabaHighlightMarkName)
    if (!mark) return
    const id = mark.attrs.id as string | undefined
    if (!id || !idToRole.has(id)) return
    const cls = getHighlightClass(idToRole.get(id)!)
    const from = pos
    const to = pos + node.nodeSize
    decorations.push(
      Decoration.inline(from, to, {
        class: `yaba-highlight ${cls}`,
        "data-highlight-id": id,
      })
    )
  })
  return decorations
}

export const HighlightDecorationsExtension = Extension.create({
  name: "highlightDecorations",

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: highlightDecorationPluginKey,
        state: {
          init(_, state) {
            const decos = mapHighlightsToDecorations(state.doc, storedHighlights)
            return DecorationSet.create(state.doc, decos)
          },
          apply(tr, pluginState) {
            const meta = tr.getMeta(HIGHLIGHTS_META_KEY)
            if (meta !== undefined) {
              const decos = mapHighlightsToDecorations(tr.doc, meta)
              return DecorationSet.create(tr.doc, decos)
            }
            if (tr.docChanged) {
              const decos = mapHighlightsToDecorations(tr.doc, storedHighlights)
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
            const highlightEl = target.closest?.(".yaba-highlight") as HTMLElement | null
            const highlightId = highlightEl?.getAttribute?.("data-highlight-id")
            if (highlightId) {
              event.preventDefault()
              const win = window as Window & {
                YabaEditorBridge?: { onHighlightTap?: (id: string) => void }
              }
              win.YabaEditorBridge?.onHighlightTap?.(highlightId)
              return true
            }
            return false
          },
        },
      }),
    ]
  },
})
