import { convertToExcalidrawElements, FONT_FAMILY, newElementWith, viewportCoordsToSceneCoords } from "@excalidraw/excalidraw"
import type { ExcalidrawImperativeAPI } from "@excalidraw/excalidraw/types"

export type ApplyCanvasInlinePayload =
  | {
      op: "setUrl"
      /** External URL, or `null` to clear */
      url: string | null
    }
  | {
      op: "setMention"
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      op: "insertTextWithUrl"
      displayText: string
      url: string
    }
  | {
      op: "insertTextWithMention"
      displayText: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      op: "clearLink"
      elementId: string
    }
  | {
      op: "setUrlOnElement"
      elementId: string
      url: string | null
    }
  | {
      op: "setMentionOnElement"
      elementId: string
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }

const MENTION_HOST = "yaba://mention"

export function encodeMentionUrl(params: {
  text: string
  bookmarkId: string
  bookmarkKindCode: number
  bookmarkLabel: string
}): string {
  const q = new URLSearchParams()
  q.set("text", params.text)
  q.set("bookmarkId", params.bookmarkId)
  q.set("bookmarkKindCode", String(params.bookmarkKindCode))
  q.set("bookmarkLabel", params.bookmarkLabel)
  return `${MENTION_HOST}?${q.toString()}`
}

export function parseMentionFromLink(link: string | null | undefined): {
  text: string
  bookmarkId: string
  bookmarkKindCode: number
  bookmarkLabel: string
} | null {
  if (link == null || link === "") return null
  if (!link.startsWith(MENTION_HOST)) return null
  try {
    const u = new URL(link)
    const text = u.searchParams.get("text") ?? ""
    const bookmarkId = u.searchParams.get("bookmarkId") ?? ""
    const bookmarkKindCode = Number(u.searchParams.get("bookmarkKindCode") ?? "0")
    const bookmarkLabel = u.searchParams.get("bookmarkLabel") ?? ""
    if (!bookmarkId) return null
    return { text, bookmarkId, bookmarkKindCode, bookmarkLabel }
  } catch {
    return null
  }
}

export function isYabaMentionLink(link: string | null | undefined): boolean {
  return link != null && link.startsWith(MENTION_HOST)
}

function sceneCoordsForNewText(api: ExcalidrawImperativeAPI): { x: number; y: number } {
  const app = api.getAppState()
  const canvasEl = document.querySelector<HTMLElement>("[data-yaba-canvas] .excalidraw__canvas")
  const rect = canvasEl?.getBoundingClientRect()
  if (rect) {
    const p = viewportCoordsToSceneCoords(
      { clientX: rect.left + rect.width / 2, clientY: rect.top + rect.height / 2 },
      app,
    )
    return { x: p.x - 48, y: p.y - 12 }
  }
  const z = app.zoom.value
  return { x: app.width / 2 / z - app.scrollX - 48, y: app.height / 2 / z - app.scrollY - 12 }
}

export function applyCanvasInlinePayload(api: ExcalidrawImperativeAPI, raw: ApplyCanvasInlinePayload): void {
  const appState = api.getAppState()
  const selectedIds = appState.selectedElementIds
  const elements = api.getSceneElements()

  const applyLinkToElements = (linkValue: string | null) => {
    const next = elements.map((el) => {
      if (!selectedIds[el.id]) return el
      return newElementWith(el as any, { link: linkValue } as any)
    })
    api.updateScene({ elements: next as any })
  }

  if (raw.op === "clearLink") {
    const next = elements.map((el) =>
      el.id === raw.elementId ? newElementWith(el as any, { link: null } as any) : el
    )
    api.updateScene({ elements: next as any })
    return
  }

  if (raw.op === "setUrlOnElement") {
    const next = elements.map((el) =>
      el.id === raw.elementId ? newElementWith(el as any, { link: raw.url } as any) : el
    )
    api.updateScene({ elements: next as any })
    return
  }

  if (raw.op === "setMentionOnElement") {
    const url = encodeMentionUrl({
      text: raw.text,
      bookmarkId: raw.bookmarkId,
      bookmarkKindCode: raw.bookmarkKindCode,
      bookmarkLabel: raw.bookmarkLabel,
    })
    const next = elements.map((el) =>
      el.id === raw.elementId ? newElementWith(el as any, { link: url } as any) : el
    )
    api.updateScene({ elements: next as any })
    return
  }

  if (raw.op === "setUrl") {
    applyLinkToElements(raw.url)
    return
  }

  if (raw.op === "setMention") {
    const url = encodeMentionUrl({
      text: raw.text,
      bookmarkId: raw.bookmarkId,
      bookmarkKindCode: raw.bookmarkKindCode,
      bookmarkLabel: raw.bookmarkLabel,
    })
    applyLinkToElements(url)
    return
  }

  const hasSelection = Object.keys(selectedIds).length > 0

  if (raw.op === "insertTextWithUrl") {
    if (hasSelection) {
      applyLinkToElements(raw.url)
      return
    }
    const { x, y } = sceneCoordsForNewText(api)
    const [textEl] = convertToExcalidrawElements(
      [
        {
          type: "text",
          text: raw.displayText,
          x,
          y,
          link: raw.url,
          fontSize: 20,
          fontFamily: FONT_FAMILY.Excalifont,
          textAlign: "center",
          verticalAlign: "middle",
        } as any,
      ],
      { regenerateIds: true }
    )
    api.updateScene({ elements: [...elements, textEl] as any })
    return
  }

  if (raw.op === "insertTextWithMention") {
    const link = encodeMentionUrl({
      text: raw.displayText,
      bookmarkId: raw.bookmarkId,
      bookmarkKindCode: raw.bookmarkKindCode,
      bookmarkLabel: raw.bookmarkLabel,
    })
    if (hasSelection) {
      applyLinkToElements(link)
      return
    }
    const { x, y } = sceneCoordsForNewText(api)
    const [textEl] = convertToExcalidrawElements(
      [
        {
          type: "text",
          text: raw.displayText,
          x,
          y,
          link,
          fontSize: 20,
          fontFamily: FONT_FAMILY.Excalifont,
          textAlign: "center",
          verticalAlign: "middle",
        } as any,
      ],
      { regenerateIds: true }
    )
    api.updateScene({ elements: [...elements, textEl] as any })
  }
}

export type CanvasSelectionLinkContext = {
  hasSelection: boolean
  selectedIds: string[]
  /** Label text for a selected text element, else empty */
  primaryText: string
  /** Raw `element.link` on first selected element */
  link: string | null
}

export function getCanvasSelectionLinkContext(api: ExcalidrawImperativeAPI): CanvasSelectionLinkContext {
  const app = api.getAppState()
  const selectedIds = Object.keys(app.selectedElementIds ?? {}).filter((id) => app.selectedElementIds[id])
  const elements = api.getSceneElements()
  const selected = elements.filter((el) => selectedIds.includes(el.id) && !el.isDeleted)
  let primaryText = ""
  let link: string | null = null
  if (selected.length > 0) {
    const first = selected[0] as any
    link = first.link ?? null
    if (first.type === "text" && typeof first.text === "string") {
      primaryText = first.text
    }
  }
  return {
    hasSelection: selectedIds.length > 0,
    selectedIds,
    primaryText,
    link,
  }
}
