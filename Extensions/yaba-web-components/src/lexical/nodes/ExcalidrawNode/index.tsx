import type {
  DOMConversionMap,
  DOMConversionOutput,
  DOMExportOutput,
  EditorConfig,
  LexicalEditor,
  LexicalNode,
  NodeKey,
  SerializedLexicalNode,
} from "lexical"
import type { JSX } from "react"
import { DecoratorNode } from "lexical"
import React from "react"

type Dimension = number | "inherit"

const ExcalidrawComponent = React.lazy(
  () => import("./ExcalidrawComponent")
)

export interface SerializedExcalidrawNode extends SerializedLexicalNode {
  data: string
  width?: Dimension
  height?: Dimension
}

function $convertExcalidrawElement(
  domNode: HTMLElement
): DOMConversionOutput | null {
  const excalidrawData = domNode.getAttribute("data-lexical-excalidraw-json")
  if (!excalidrawData) return null
  const styleAttributes = window.getComputedStyle(domNode)
  const heightStr = styleAttributes.getPropertyValue("height")
  const widthStr = styleAttributes.getPropertyValue("width")
  const height =
    !heightStr || heightStr === "inherit" ? "inherit" : parseInt(heightStr, 10)
  const width =
    !widthStr || widthStr === "inherit" ? "inherit" : parseInt(widthStr, 10)
  const node = $createExcalidrawNode(excalidrawData, width, height)
  return { node }
}

export class ExcalidrawNode extends DecoratorNode<JSX.Element> {
  __data: string
  __width: Dimension
  __height: Dimension

  static getType(): string {
    return "excalidraw"
  }

  static clone(node: ExcalidrawNode): ExcalidrawNode {
    return new ExcalidrawNode(
      node.__data,
      node.__width,
      node.__height,
      node.__key
    )
  }

  static importJSON(serializedNode: SerializedExcalidrawNode): ExcalidrawNode {
    const node = new ExcalidrawNode(
      serializedNode.data,
      serializedNode.width ?? "inherit",
      serializedNode.height ?? "inherit"
    )
    return node
  }

  exportJSON(): SerializedExcalidrawNode {
    return {
      ...super.exportJSON(),
      data: this.__data,
      height: this.__height === "inherit" ? undefined : this.__height,
      width: this.__width === "inherit" ? undefined : this.__width,
    }
  }

  constructor(
    data = "[]",
    width: Dimension = "inherit",
    height: Dimension = "inherit",
    key?: NodeKey
  ) {
    super(key)
    this.__data = data
    this.__width = width
    this.__height = height
  }

  createDOM(config: EditorConfig): HTMLElement {
    const span = document.createElement("span")
    const theme = config.theme
    const className = (theme as { image?: string }).image
    if (className !== undefined) {
      span.className = className
    }
    return span
  }

  updateDOM(): false {
    return false
  }

  static importDOM(): DOMConversionMap | null {
    return {
      span: (domNode: HTMLElement) => {
        if (
          !(domNode as HTMLSpanElement).hasAttribute("data-lexical-excalidraw-json")
        ) {
          return null
        }
        return {
          conversion: $convertExcalidrawElement,
          priority: 1,
        }
      },
    }
  }

  exportDOM(editor: LexicalEditor): DOMExportOutput {
    const element = document.createElement("span")
    element.style.display = "inline-block"
    const content = editor.getElementByKey(this.getKey())
    if (content !== null) {
      const svg = content.querySelector("svg")
      if (svg !== null) {
        element.innerHTML = svg.outerHTML
      }
    }
    element.style.width =
      this.__width === "inherit" ? "inherit" : `${this.__width}px`
    element.style.height =
      this.__height === "inherit" ? "inherit" : `${this.__height}px`
    element.setAttribute("data-lexical-excalidraw-json", this.__data)
    return { element }
  }

  setData(data: string): void {
    const self = this.getWritable()
    self.__data = data
  }

  getWidth(): Dimension {
    return this.getLatest().__width
  }

  setWidth(width: Dimension): void {
    const self = this.getWritable()
    self.__width = width
  }

  getHeight(): Dimension {
    return this.getLatest().__height
  }

  setHeight(height: Dimension): void {
    const self = this.getWritable()
    self.__height = height
  }

  decorate(_editor: LexicalEditor, _config: EditorConfig): JSX.Element {
    return (
      <React.Suspense fallback={null}>
        <ExcalidrawComponent
          nodeKey={this.getKey()}
          data={this.__data}
          width={this.__width}
          height={this.__height}
        />
      </React.Suspense>
    )
  }
}

export function $createExcalidrawNode(
  data = "[]",
  width: Dimension = "inherit",
  height: Dimension = "inherit"
): ExcalidrawNode {
  return new ExcalidrawNode(data, width, height)
}

export function $isExcalidrawNode(
  node: LexicalNode | null | undefined
): node is ExcalidrawNode {
  return node instanceof ExcalidrawNode
}
