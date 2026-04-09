import { useEffect, useState } from "react"
import type { ExcalidrawImperativeAPI } from "@excalidraw/excalidraw/types"
import { sceneCoordsToViewportCoords } from "@excalidraw/excalidraw"
import { postToYabaNativeHost } from "@/bridge/yaba-native-host"
import { isYabaMentionLink, parseMentionFromLink } from "@/bridge/canvas-inline"

type BadgeItem = {
  elementId: string
  x: number
  y: number
  kind: "link" | "mention"
  text: string
  url: string
  bookmarkId?: string
  bookmarkKindCode?: number
  bookmarkLabel?: string
}

export function CanvasLinkBadgeLayer(props: {
  api: ExcalidrawImperativeAPI | null
  revision: number
}) {
  const { api, revision } = props
  const [badges, setBadges] = useState<BadgeItem[]>([])

  useEffect(() => {
    if (!api) {
      setBadges([])
      return
    }
    const app = api.getAppState()
    const elements = api.getSceneElements()
    const next: BadgeItem[] = []
    for (const el of elements) {
      if (el.isDeleted) continue
      const link = (el as { link?: string | null }).link
      if (link == null || link === "") continue
      const anyEl = el as { id: string; x: number; y: number; width?: number; height?: number }
      const w = anyEl.width ?? 0
      const h = anyEl.height ?? 0
      const vx = anyEl.x + w
      const vy = anyEl.y + Math.min(18, h * 0.15)
      const vp = sceneCoordsToViewportCoords({ sceneX: vx, sceneY: vy }, app)
      let label = ""
      if ((el as { type?: string }).type === "text" && typeof (el as { text?: string }).text === "string") {
        label = (el as { text: string }).text
      }
      const mention = parseMentionFromLink(link)
      if (mention) {
        next.push({
          elementId: anyEl.id,
          x: vp.x,
          y: vp.y,
          kind: "mention",
          text: mention.text,
          url: link,
          bookmarkId: mention.bookmarkId,
          bookmarkKindCode: mention.bookmarkKindCode,
          bookmarkLabel: mention.bookmarkLabel,
        })
      } else if (isYabaMentionLink(link)) {
        // Malformed yaba mention URL — fall back to plain link behavior
        next.push({
          elementId: anyEl.id,
          x: vp.x,
          y: vp.y,
          kind: "link",
          text: label,
          url: link,
        })
      } else {
        next.push({
          elementId: anyEl.id,
          x: vp.x,
          y: vp.y,
          kind: "link",
          text: label,
          url: link,
        })
      }
    }
    setBadges(next)
  }, [api, revision])

  if (!api || badges.length === 0) return null

  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        pointerEvents: "none",
        zIndex: 5,
      }}
    >
      {badges.map((b) => (
        <button
          key={b.elementId}
          type="button"
          style={{
            position: "absolute",
            left: b.x,
            top: b.y,
            width: 26,
            height: 26,
            padding: 0,
            border: "none",
            borderRadius: 6,
            background: "rgba(255,255,255,0.92)",
            boxShadow: "0 1px 3px rgba(0,0,0,0.2)",
            pointerEvents: "auto",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            transform: "translate(-4px, -4px)",
          }}
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => {
            e.preventDefault()
            e.stopPropagation()
            if (b.kind === "mention" && b.bookmarkId) {
              postToYabaNativeHost({
                type: "canvasMentionTap",
                elementId: b.elementId,
                text: b.text,
                bookmarkId: b.bookmarkId,
                bookmarkKindCode: b.bookmarkKindCode ?? 0,
                bookmarkLabel: b.bookmarkLabel ?? "",
              })
            } else {
              postToYabaNativeHost({
                type: "canvasLinkTap",
                elementId: b.elementId,
                text: b.text,
                url: b.url,
              })
            }
          }}
          aria-label={b.kind === "mention" ? "Mention" : "Link"}
        >
          {b.kind === "mention" ? <AtIcon /> : <LinkIcon />}
        </button>
      ))}
    </div>
  )
}

function LinkIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M10.5 13.5L13.5 10.5M9 15l-1.5 1.5a4 4 0 0 1-5.66-5.66L4.5 9M15 9l1.5-1.5a4 4 0 0 1 5.66 5.66L19.5 15"
        stroke="#2563eb"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  )
}

function AtIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 14.5a2.5 2.5 0 0 0 2.45-2L16 9.5A6 6 0 1 0 9 19h.5"
        stroke="#2563eb"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path d="M16 19v-3" stroke="#2563eb" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}
