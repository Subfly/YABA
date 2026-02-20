import type { LexicalEditor } from "lexical"
import { calculateZoomLevel } from "@lexical/utils"
import { useRef } from "react"
import "./ImageResizer.css"

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

const Direction = {
  east: 1 << 0,
  north: 1 << 3,
  south: 1 << 1,
  west: 1 << 2,
}

interface ImageResizerProps {
  editor: LexicalEditor
  imageRef: React.RefObject<HTMLElement | null>
  maxWidth?: number
  onResizeEnd: (width: "inherit" | number, height: "inherit" | number) => void
  onResizeStart: () => void
}

export default function ImageResizer({
  onResizeStart,
  onResizeEnd,
  imageRef,
  maxWidth,
  editor,
}: ImageResizerProps) {
  const controlWrapperRef = useRef<HTMLDivElement>(null)
  const userSelect = useRef({ priority: "", value: "default" })
  const positioningRef = useRef<{
    currentHeight: "inherit" | number
    currentWidth: "inherit" | number
    direction: number
    isResizing: boolean
    ratio: number
    startHeight: number
    startWidth: number
    startX: number
    startY: number
  }>({
    currentHeight: 0,
    currentWidth: 0,
    direction: 0,
    isResizing: false,
    ratio: 0,
    startHeight: 0,
    startWidth: 0,
    startX: 0,
    startY: 0,
  })

  const editorRootElement = editor.getRootElement()
  const maxWidthContainer = maxWidth
    ? maxWidth
    : editorRootElement !== null
      ? editorRootElement.getBoundingClientRect().width - 20
      : 100
  const maxHeightContainer =
    editorRootElement !== null
      ? editorRootElement.getBoundingClientRect().height - 20
      : 100
  const minWidth = 100
  const minHeight = 100

  const setStartCursor = (direction: number) => {
    const ew = direction === Direction.east || direction === Direction.west
    const ns = direction === Direction.north || direction === Direction.south
    const nwse =
      (direction & Direction.north && direction & Direction.west) ||
      (direction & Direction.south && direction & Direction.east)
    const cursorDir = ew ? "ew-resize" : ns ? "ns-resize" : nwse ? "nwse-resize" : "nesw-resize"
    if (editorRootElement !== null) {
      editorRootElement.style.setProperty("cursor", `${cursorDir}`, "important")
    }
    if (document.body !== null) {
      document.body.style.setProperty("cursor", `${cursorDir}`, "important")
      userSelect.current.value = document.body.style.getPropertyValue("-webkit-user-select")
      userSelect.current.priority = document.body.style.getPropertyPriority("-webkit-user-select")
      document.body.style.setProperty("-webkit-user-select", "none", "important")
    }
  }

  const setEndCursor = () => {
    if (editorRootElement !== null) {
      editorRootElement.style.setProperty("cursor", "text")
    }
    if (document.body !== null) {
      document.body.style.setProperty("cursor", "default")
      document.body.style.setProperty("-webkit-user-select", userSelect.current.value, userSelect.current.priority)
    }
  }

  const handlePointerDown = (event: React.PointerEvent, direction: number) => {
    if (!editor.isEditable()) return
    const image = imageRef.current
    const controlWrapper = controlWrapperRef.current
    if (image !== null && controlWrapper !== null) {
      event.preventDefault()
      const { width, height } = image.getBoundingClientRect()
      const zoom = calculateZoomLevel(image)
      const positioning = positioningRef.current
      positioning.startWidth = width
      positioning.startHeight = height
      positioning.ratio = width / height
      positioning.currentWidth = width
      positioning.currentHeight = height
      positioning.startX = event.clientX / zoom
      positioning.startY = event.clientY / zoom
      positioning.isResizing = true
      positioning.direction = direction
      setStartCursor(direction)
      onResizeStart()
      controlWrapper.classList.add("image-control-wrapper--resizing")
      image.style.height = `${height}px`
      image.style.width = `${width}px`
      const handlePointerMove = (e: PointerEvent) => {
        const img = imageRef.current
        const pos = positioningRef.current
        const isHorizontal = pos.direction & (Direction.east | Direction.west)
        const isVertical = pos.direction & (Direction.south | Direction.north)
        if (img !== null && pos.isResizing) {
          const z = calculateZoomLevel(img)
          if (isHorizontal && isVertical) {
            let diff = Math.floor(pos.startX - e.clientX / z)
            diff = pos.direction & Direction.east ? -diff : diff
            const width = clamp(pos.startWidth + diff, minWidth, maxWidthContainer)
            const height = width / pos.ratio
            img.style.width = `${width}px`
            img.style.height = `${height}px`
            pos.currentHeight = height
            pos.currentWidth = width
          } else if (isVertical) {
            let diff = Math.floor(pos.startY - e.clientY / z)
            diff = pos.direction & Direction.south ? -diff : diff
            const height = clamp(pos.startHeight + diff, minHeight, maxHeightContainer)
            img.style.height = `${height}px`
            pos.currentHeight = height
          } else {
            let diff = Math.floor(pos.startX - e.clientX / z)
            diff = pos.direction & Direction.east ? -diff : diff
            const width = clamp(pos.startWidth + diff, minWidth, maxWidthContainer)
            img.style.width = `${width}px`
            pos.currentWidth = width
          }
        }
      }
      const handlePointerUp = () => {
        const img = imageRef.current
        const pos = positioningRef.current
        const cw = controlWrapperRef.current
        if (img !== null && cw !== null && pos.isResizing) {
          const width = pos.currentWidth
          const height = pos.currentHeight
          pos.startWidth = 0
          pos.startHeight = 0
          pos.ratio = 0
          pos.startX = 0
          pos.startY = 0
          pos.currentWidth = 0
          pos.currentHeight = 0
          pos.isResizing = false
          cw.classList.remove("image-control-wrapper--resizing")
          setEndCursor()
          onResizeEnd(width, height)
        }
        document.removeEventListener("pointermove", handlePointerMove)
        document.removeEventListener("pointerup", handlePointerUp)
      }
      document.addEventListener("pointermove", handlePointerMove)
      document.addEventListener("pointerup", handlePointerUp)
    }
  }

  return (
    <div ref={controlWrapperRef} className="image-control-wrapper">
      <div
        className="resizer n"
        onPointerDown={(e) => handlePointerDown(e, Direction.north)}
        style={{ cursor: "n-resize" }}
      />
      <div
        className="resizer ne"
        onPointerDown={(e) => handlePointerDown(e, Direction.north | Direction.east)}
        style={{ cursor: "ne-resize" }}
      />
      <div
        className="resizer e"
        onPointerDown={(e) => handlePointerDown(e, Direction.east)}
        style={{ cursor: "e-resize" }}
      />
      <div
        className="resizer se"
        onPointerDown={(e) => handlePointerDown(e, Direction.south | Direction.east)}
        style={{ cursor: "se-resize" }}
      />
      <div
        className="resizer s"
        onPointerDown={(e) => handlePointerDown(e, Direction.south)}
        style={{ cursor: "s-resize" }}
      />
      <div
        className="resizer sw"
        onPointerDown={(e) => handlePointerDown(e, Direction.south | Direction.west)}
        style={{ cursor: "sw-resize" }}
      />
      <div
        className="resizer w"
        onPointerDown={(e) => handlePointerDown(e, Direction.west)}
        style={{ cursor: "w-resize" }}
      />
      <div
        className="resizer nw"
        onPointerDown={(e) => handlePointerDown(e, Direction.north | Direction.west)}
        style={{ cursor: "nw-resize" }}
      />
    </div>
  )
}
