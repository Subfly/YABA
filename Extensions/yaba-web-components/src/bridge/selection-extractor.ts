import type { Editor } from "@tiptap/core"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"

export interface SelectionSnapshot {
  startSectionKey: string
  startOffsetInSection: number
  endSectionKey: string
  endOffsetInSection: number
  selectedText: string
  prefixText?: string
  suffixText?: string
}

interface BlockInfo {
  key: string
  from: number
  to: number
  text: string
}

/**
 * Extracts a normalized selection snapshot from the editor for highlight creation.
 * Returns null if there is no valid text selection.
 */
export function getSelectionSnapshot(editor: Editor | null): SelectionSnapshot | null {
  if (!editor) return null

  const { state } = editor
  const { doc, selection } = state
  const { from, to } = selection

  if (from === to) return null

  const blocks = buildBlockIndex(doc)
  if (blocks.length === 0) return null

  const startBlock = findBlockAtPos(blocks, from)
  const endBlock = findBlockAtPos(blocks, to)
  if (!startBlock || !endBlock) return null

  const selectedText = doc.textBetween(from, to, "\n")
  if (!selectedText.trim()) return null

  const startOffsetInSection = from - startBlock.from
  const endOffsetInSection = to - endBlock.from

  const prefixLength = 30
  const suffixLength = 30
  const rawPrefix =
    startOffsetInSection > 0
      ? startBlock.text.slice(Math.max(0, startOffsetInSection - prefixLength), startOffsetInSection)
      : ""
  const rawSuffix =
    endOffsetInSection < endBlock.text.length
      ? endBlock.text.slice(
          endOffsetInSection,
          Math.min(endBlock.text.length, endOffsetInSection + suffixLength)
        )
      : ""

  return {
    startSectionKey: startBlock.key,
    startOffsetInSection,
    endSectionKey: endBlock.key,
    endOffsetInSection,
    selectedText: selectedText.trim(),
    prefixText: rawPrefix.trim() || undefined,
    suffixText: rawSuffix.trim() || undefined,
  }
}

function buildBlockIndex(doc: ProseMirrorNode): BlockInfo[] {
  const blocks: BlockInfo[] = []
  let index = 0
  doc.descendants((node, pos) => {
    if (node.isBlock && node.isTextblock) {
      const text = node.textContent
      blocks.push({
        key: `block-${index}`,
        from: pos + 1,
        to: pos + node.nodeSize - 1,
        text,
      })
      index++
    }
  })
  return blocks
}

function findBlockAtPos(blocks: BlockInfo[], pos: number): BlockInfo | null {
  for (const block of blocks) {
    if (pos >= block.from && pos <= block.to) return block
  }
  return null
}
