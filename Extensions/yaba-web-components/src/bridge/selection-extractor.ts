import type { Editor } from "@tiptap/core"

export interface SelectionSnapshot {
  selectedText: string
  prefixText?: string
  suffixText?: string
}

/**
 * Extracts quote context from the editor for highlight creation (no positional anchors).
 * Returns null if there is no valid text selection.
 */
export function getSelectionSnapshot(editor: Editor | null): SelectionSnapshot | null {
  if (!editor) return null

  const { state } = editor
  const { doc, selection } = state
  const { from, to } = selection

  if (from === to) return null

  const selectedText = doc.textBetween(from, to, "\n")
  if (!selectedText.trim()) return null

  const $from = doc.resolve(from)
  const $to = doc.resolve(to)
  const prefixLength = 30
  const suffixLength = 30
  const prefixStart = Math.max($from.start(), from - prefixLength)
  const suffixEnd = Math.min($to.end(), to + suffixLength)
  const rawPrefix = doc.textBetween(prefixStart, from, "\n")
  const rawSuffix = doc.textBetween(to, suffixEnd, "\n")

  return {
    selectedText: selectedText.trim(),
    prefixText: rawPrefix.trim() || undefined,
    suffixText: rawSuffix.trim() || undefined,
  }
}
