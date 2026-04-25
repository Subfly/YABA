import type { SelectionSnapshot } from "./selection-snapshot"

/**
 * Quote context for highlight creation (DOM read-it-later), same shape as the TipTap editor path.
 */
export function getDomSelectionSnapshot(contentRoot: HTMLElement | null): SelectionSnapshot | null {
  if (!contentRoot) return null
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return null
  const range = sel.getRangeAt(0)
  if (range.collapsed) return null
  if (!contentRoot.contains(range.commonAncestorContainer)) return null

  const selectedText = sel.toString()
  if (!selectedText.trim()) return null

  const beforeRange = document.createRange()
  beforeRange.setStartBefore(contentRoot)
  beforeRange.setEnd(range.startContainer, range.startOffset)
  const rawPrefix = beforeRange.toString()
  const prefixText = rawPrefix.trim().length > 0 ? rawPrefix.trim().slice(-30) : undefined

  const afterRange = document.createRange()
  afterRange.setStart(range.endContainer, range.endOffset)
  afterRange.setEnd(contentRoot, contentRoot.childNodes.length)
  const rawSuffix = afterRange.toString()
  const suffixText = rawSuffix.trim().length > 0 ? rawSuffix.trim().slice(0, 30) : undefined

  return {
    selectedText: selectedText.trim(),
    prefixText: prefixText || undefined,
    suffixText: suffixText || undefined,
  }
}
