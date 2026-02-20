import type { DOMConversionMap, DOMConversionOutput, DOMExportOutput, EditorConfig, LexicalEditor, LexicalNode, NodeKey, SerializedLexicalNode } from "lexical"
import { $applyNodeReplacement, DecoratorNode } from "lexical"
import type { ReactElement } from "react"

export interface SerializedImageNode extends SerializedLexicalNode {
  alt: string
  src: string
  title?: string
}

export class ImageNode extends DecoratorNode<ReactElement> {
  __src: string
  __alt: string
  __title?: string

  static getType(): string {
    return "image"
  }

  static clone(node: ImageNode): ImageNode {
    return new ImageNode(node.__src, node.__alt, node.__title, node.__key)
  }

  constructor(src: string, alt = "", title?: string, key?: NodeKey) {
    super(key)
    this.__src = src
    this.__alt = alt
    this.__title = title
  }

  createDOM(_config: EditorConfig): HTMLElement {
    const span = document.createElement("span")
    span.className = "yaba-editor-image-wrapper"
    return span
  }

  updateDOM(): false {
    return false
  }

  decorate(_editor: LexicalEditor, _config: EditorConfig): ReactElement {
    return (
      <ImageComponent
        src={this.__src}
        alt={this.__alt}
        title={this.__title}
      />
    )
  }

  getSrc(): string {
    return this.__src
  }

  getAlt(): string {
    return this.__alt
  }

  getTitle(): string | undefined {
    return this.__title
  }

  setSrc(src: string): this {
    const self = this.getWritable()
    self.__src = src
    return self
  }

  setAlt(alt: string): this {
    const self = this.getWritable()
    self.__alt = alt
    return self
  }

  exportJSON(): SerializedImageNode {
    return {
      alt: this.__alt,
      src: this.__src,
      title: this.__title,
      type: "image",
      version: 1,
    }
  }

  static importJSON(serialized: SerializedImageNode): ImageNode {
    return $createImageNode(
      serialized.src,
      serialized.alt,
      serialized.title
    )
  }

  static importDOM(): DOMConversionMap | null {
    return {
      img: () => ({
        conversion: (element: HTMLElement): DOMConversionOutput | null => {
          const dom = element as HTMLImageElement
          const src = dom.getAttribute("src")
          const alt = dom.getAttribute("alt") ?? ""
          const title = dom.getAttribute("title") ?? undefined
          if (!src) return null
          return { node: $createImageNode(src, alt, title) }
        },
        priority: 0,
      }),
    }
  }

  exportDOM(): DOMExportOutput {
    const img = document.createElement("img")
    img.setAttribute("src", this.__src)
    img.setAttribute("alt", this.__alt)
    if (this.__title) img.setAttribute("title", this.__title)
    img.style.maxWidth = "100%"
    img.style.height = "auto"
    img.style.borderRadius = "8px"
    return { element: img }
  }

  isInline(): false {
    return false
  }
}

function ImageComponent({
  src,
  alt,
  title,
}: {
  src: string
  alt: string
  title?: string
}) {
  return (
    <img
      src={src}
      alt={alt}
      title={title}
      style={{
        maxWidth: "100%",
        height: "auto",
        borderRadius: "8px",
        display: "block",
      }}
      draggable={false}
    />
  )
}

export function $createImageNode(
  src: string,
  alt = "",
  title?: string
): ImageNode {
  return $applyNodeReplacement(new ImageNode(src, alt, title))
}

export function $isImageNode(
  node: LexicalNode | null | undefined
): node is ImageNode {
  return node instanceof ImageNode
}
