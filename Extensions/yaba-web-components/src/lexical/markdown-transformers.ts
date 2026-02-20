import type { TextMatchTransformer } from "@lexical/markdown"
import { ImageNode, $createImageNode, $isImageNode } from "./nodes/ImageNode"

const IMAGE_IMPORT_REGEXP = /!\[([^[]*)\]\(([^)]+)\)/
const IMAGE_REGEXP = /!\[([^[]*)\]\(([^)]+)\)$/

export const IMAGE: TextMatchTransformer = {
  dependencies: [ImageNode],
  export: (node) => {
    if (!$isImageNode(node)) return null
    const alt = node.getAlt().replace(/\]/g, "\\]")
    return `![${alt}](${node.getSrc()})`
  },
  importRegExp: IMAGE_IMPORT_REGEXP,
  regExp: IMAGE_REGEXP,
  replace: (textNode, match) => {
    const alt = match[1] ?? ""
    const src = match[2] ?? ""
    if (!src) return
    const imageNode = $createImageNode(src, alt)
    textNode.replace(imageNode)
  },
  trigger: ")",
  type: "text-match",
}
