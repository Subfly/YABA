import type { ExcalidrawElement } from "@excalidraw/excalidraw/element/types"
import type { AppState, BinaryFiles } from "@excalidraw/excalidraw/types"
import { exportToSvg } from "@excalidraw/excalidraw"
import { useEffect, useState } from "react"

type Dimension = "inherit" | number

interface ExcalidrawImageProps {
  appState: AppState
  className?: string
  elements: readonly ExcalidrawElement[]
  files: BinaryFiles
  height?: Dimension
  imageContainerRef: React.RefObject<HTMLDivElement | null>
  width?: Dimension
}

function removeStyleFromSvg(svg: SVGElement) {
  const styleTag = svg?.firstElementChild?.firstElementChild
  const viewBox = svg.getAttribute("viewBox")
  if (viewBox != null) {
    const viewBoxDimensions = viewBox.split(" ")
    svg.setAttribute("width", viewBoxDimensions[2])
    svg.setAttribute("height", viewBoxDimensions[3])
  }
  if (styleTag && styleTag.tagName === "style") {
    styleTag.remove()
  }
}

export default function ExcalidrawImage({
  elements,
  files,
  imageContainerRef,
  appState,
  width = "inherit",
  height = "inherit",
}: ExcalidrawImageProps) {
  const [svg, setSvg] = useState<SVGElement | null>(null)

  useEffect(() => {
    const setContent = async () => {
      const svgEl: SVGElement = await exportToSvg({
        appState,
        elements,
        files,
      })
      removeStyleFromSvg(svgEl)
      svgEl.setAttribute("width", "100%")
      svgEl.setAttribute("height", "100%")
      svgEl.setAttribute("display", "block")
      setSvg(svgEl)
    }
    setContent()
  }, [elements, files, appState])

  const containerStyle: React.CSSProperties = {}
  if (width !== "inherit") {
    containerStyle.width = `${width}px`
  }
  if (height !== "inherit") {
    containerStyle.height = `${height}px`
  }

  return (
    <div
      ref={(node) => {
        if (node && imageContainerRef) {
          ;(imageContainerRef as React.MutableRefObject<HTMLDivElement | null>).current = node
        }
      }}
      className=""
      style={containerStyle}
      dangerouslySetInnerHTML={{ __html: svg?.outerHTML ?? "" }}
    />
  )
}
