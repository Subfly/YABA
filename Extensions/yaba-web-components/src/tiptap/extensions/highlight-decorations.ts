import { Extension } from "@tiptap/core"
import { Plugin, PluginKey } from "@tiptap/pm/state"
import { Decoration, DecorationSet } from "@tiptap/pm/view"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"

export const HIGHLIGHTS_META_KEY = "yaba-highlights"
export const highlightDecorationPluginKey = new PluginKey("highlightDecorations")

export interface HighlightForRendering {
  id: string
  startSectionKey: string
  startOffsetInSection: number
  endSectionKey: string
  endOffsetInSection: number
  colorRole: string
}

let storedHighlights: HighlightForRendering[] = []

export function setStoredHighlights(highlights: HighlightForRendering[]): void {
  storedHighlights = highlights
}

export function getStoredHighlights(): HighlightForRendering[] {
  return storedHighlights
}

interface BlockInfo {
  key: string
  from: number
  to: number
}

function buildBlockIndex(doc: ProseMirrorNode): BlockInfo[] {
  const blocks: BlockInfo[] = []
  let index = 0
  doc.descendants((node, pos) => {
    if (node.isBlock && node.isTextblock) {
      blocks.push({
        key: `block-${index}`,
        from: pos + 1,
        to: pos + node.nodeSize - 1,
      })
      index++
    }
  })
  return blocks
}

function findBlockByKey(blocks: BlockInfo[], key: string): BlockInfo | null {
  return blocks.find((b) => b.key === key) ?? null
}

export function getHighlightRanges(
  doc: ProseMirrorNode,
  highlights: HighlightForRendering[]
): { from: number; to: number }[] {
  const blocks = buildBlockIndex(doc)
  const ranges: { from: number; to: number }[] = []
  for (const h of highlights) {
    const startBlock = findBlockByKey(blocks, h.startSectionKey)
    const endBlock = findBlockByKey(blocks, h.endSectionKey)
    if (!startBlock || !endBlock) continue
    const from = startBlock.from + h.startOffsetInSection
    const to = endBlock.from + h.endOffsetInSection
    if (from < to) ranges.push({ from, to })
  }
  return ranges
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

function mapHighlightsToDecorations(
  doc: ProseMirrorNode,
  highlights: HighlightForRendering[]
): Decoration[] {
  const blocks = buildBlockIndex(doc)
  const decorations: Decoration[] = []

  for (const h of highlights) {
    const startBlock = findBlockByKey(blocks, h.startSectionKey)
    const endBlock = findBlockByKey(blocks, h.endSectionKey)
    if (!startBlock || !endBlock) continue

    const from = startBlock.from + h.startOffsetInSection
    const to = endBlock.from + h.endOffsetInSection
    if (from >= to) continue

    const cls = getHighlightClass(h.colorRole)
    decorations.push(
      Decoration.inline(from, to, {
        class: `yaba-highlight ${cls}`,
        "data-highlight-id": h.id,
      })
    )
  }
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
            const decos = mapHighlightsToDecorations(
              state.doc,
              storedHighlights
            )
            return DecorationSet.create(state.doc, decos)
          },
          apply(tr, pluginState) {
            const meta = tr.getMeta(HIGHLIGHTS_META_KEY)
            if (meta !== undefined) {
              const decos = mapHighlightsToDecorations(tr.doc, meta)
              return DecorationSet.create(tr.doc, decos)
            }
            if (tr.docChanged) {
              return pluginState.map(tr.mapping, tr.doc)
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
              const win = window as Window & { YabaEditorBridge?: { onHighlightTap?: (id: string) => void } }
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
