import { useEffect, useState } from "react"
import type { ExcalidrawImperativeAPI } from "@excalidraw/excalidraw/types"
import { sceneCoordsToViewportCoords } from "@excalidraw/excalidraw"
import { postToYabaNativeHost } from "@/bridge/yaba-native-host"
import { isYabaMentionLink, parseMentionFromLink } from "@/bridge/canvas-inline"
import linkSquareIcon from "@/assets/link-square-01.svg"
import mentionIcon from "@/assets/arrow-turn-backward.svg"

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
            width: 24,
            height: 24,
            padding: 0,
            border: "none",
            borderRadius: 0,
            background: "transparent",
            boxShadow: "none",
            pointerEvents: "auto",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            transform: "translate(-2px, -2px)",
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
          <img
            src={b.kind === "mention" ? mentionIcon : linkSquareIcon}
            alt=""
            aria-hidden
            width={24}
            height={24}
            style={{ display: "block", pointerEvents: "none" }}
          />
        </button>
      ))}
    </div>
  )
}
