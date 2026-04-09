/**
 * Context menu items hidden via CSS in `canvas-host.css`.
 * Keep this list in sync when changing either file.
 */
export const CONTEXT_MENU_HIDDEN_TEST_IDS: ReadonlySet<string> = new Set([
  "copyAsPng",
  "copyAsSvg",
  "copyText",
  "copyStyles",
  "pasteStyles",
  "addToLibrary",
  "viewMode",
  "toggleCanvasMenu",
  "toggleEditMenu",
])

function collapseRedundantSeparators(menu: HTMLElement): void {
  const hiddenLi = (el: Element): boolean => {
    if (el.tagName !== "LI") return false
    const tid = el.getAttribute("data-testid")
    return tid != null && CONTEXT_MENU_HIDDEN_TEST_IDS.has(tid)
  }

  let outer = true
  while (outer) {
    outer = false
    const kids = Array.from(menu.children)

    for (let i = 0; i < kids.length; i++) {
      const el = kids[i]
      if (el.tagName !== "HR") continue

      const segment: Element[] = []
      let j = i + 1
      while (j < kids.length && kids[j].tagName !== "HR") {
        segment.push(kids[j])
        j++
      }
      const nextHr = j < kids.length ? kids[j] : null

      const onlyHiddenLis =
        segment.length > 0 && segment.every((n) => n.tagName === "LI" && hiddenLi(n))

      if (onlyHiddenLis) {
        el.remove()
        nextHr?.remove()
        outer = true
        break
      }
    }
  }

  for (let guard = 0; guard < 32; guard++) {
    const kids = Array.from(menu.children)
    let removed = false
    for (let i = 1; i < kids.length; i++) {
      if (kids[i].tagName === "HR" && kids[i - 1].tagName === "HR") {
        kids[i].remove()
        removed = true
        break
      }
    }
    if (!removed) break
  }
}

/**
 * Removes orphan/double `hr` separators after items are hidden with CSS.
 */
export function installCanvasContextMenuSeparatorCleanup(root: HTMLElement): () => void {
  const run = (): void => {
    root.querySelectorAll<HTMLElement>(".context-menu").forEach((menu) => {
      collapseRedundantSeparators(menu)
    })
  }

  const observer = new MutationObserver(() => {
    requestAnimationFrame(run)
  })
  observer.observe(root, { subtree: true, childList: true })
  requestAnimationFrame(run)

  return () => observer.disconnect()
}
