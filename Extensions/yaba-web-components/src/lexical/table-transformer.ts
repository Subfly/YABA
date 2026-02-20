import type { MultilineElementTransformer } from "@lexical/markdown"
import { $createTextNode, $isElementNode, $isTextNode } from "lexical"
import {
  TableNode,
  TableCellNode,
  TableRowNode,
  $createTableNodeWithDimensions,
  $isTableNode,
} from "@lexical/table"

const TABLE_ROW_REGEXP = /^\|(.+)\|\s?$/
const TABLE_DIVIDER_REGEXP = /^\|[\s:\-|]+\|\s?$/

function parseTableCellContent(cell: string): string {
  return cell.trim().replace(/\\\|/g, "|")
}

function parseTableRows(lines: string[]): { header: string[]; rows: string[][] } | null {
  if (lines.length < 2) return null
  const firstLine = lines[0]
  const secondLine = lines[1]
  const firstMatch = firstLine.match(TABLE_ROW_REGEXP)
  const secondMatch = secondLine.match(TABLE_DIVIDER_REGEXP)
  if (!firstMatch || !secondMatch) return null

  const headerCells = firstMatch[1].split("|").map((c) => parseTableCellContent(c))
  const columnCount = headerCells.length
  const rows: string[][] = [headerCells]

  for (let i = 2; i < lines.length; i++) {
    const line = lines[i]
    const match = line.match(TABLE_ROW_REGEXP)
    if (!match) break
    const cells = match[1].split("|").map((c) => parseTableCellContent(c))
    if (cells.length !== columnCount) break
    rows.push(cells)
  }

  return {
    header: rows[0],
    rows: rows.slice(1),
  }
}

export const TABLE: MultilineElementTransformer = {
  dependencies: [TableNode, TableRowNode, TableCellNode],
  type: "multiline-element",
  regExpStart: TABLE_ROW_REGEXP,
  handleImportAfterStartMatch: ({ lines, rootNode, startLineIndex }) => {
    const slice = lines.slice(startLineIndex)
    const parsed = parseTableRows(slice)
    if (!parsed || parsed.rows.length === 0) return null

    const { header, rows } = parsed
    const columnCount = header.length
    const rowCount = 1 + rows.length
    const tableNode = $createTableNodeWithDimensions(rowCount, columnCount, {
      rows: true,
      columns: false,
    })

    const tableRows = tableNode.getChildren()
    for (let r = 0; r < tableRows.length; r++) {
      const rowNode = tableRows[r] as TableRowNode
      const cellNodes = rowNode.getChildren()
      const sourceRow = r === 0 ? header : rows[r - 1]
      for (let c = 0; c < cellNodes.length; c++) {
        const cellNode = cellNodes[c] as TableCellNode
        const text = sourceRow[c] ?? ""
        const paragraph = cellNode.getFirstChild()
        if ($isElementNode(paragraph)) {
          const existingText = paragraph.getFirstChild()
          if (existingText && $isTextNode(existingText)) {
            existingText.replace($createTextNode(text))
          } else {
            paragraph.append($createTextNode(text))
          }
        }
      }
    }

    rootNode.append(tableNode)
    const lastLineIndex = startLineIndex + 1 + rows.length
    return [true, lastLineIndex]
  },
  export: (node, traverseChildren) => {
    if (!$isTableNode(node)) return null
    const rows: string[] = []
    for (const rowNode of node.getChildren()) {
      if (!$isTableRowNode(rowNode)) continue
      const cells: string[] = []
      for (const cellNode of rowNode.getChildren()) {
        if (!$isTableCellNode(cellNode)) continue
        const text = traverseChildren(cellNode as import("lexical").ElementNode).replace(/\|/g, "\\|").trim()
        cells.push(text)
      }
      if (cells.length > 0) rows.push("| " + cells.join(" | ") + " |")
    }
    if (rows.length < 2) return null
    const columnCount = rows[0].split("|").filter(Boolean).length
    const divider = "| " + Array(columnCount).fill("---").join(" | ") + " |"
    return "\n" + rows[0] + "\n" + divider + "\n" + rows.slice(1).join("\n") + "\n"
  },
  replace: () => false,
}

function $isTableRowNode(node: unknown): node is TableRowNode {
  return node instanceof TableRowNode
}

function $isTableCellNode(node: unknown): node is TableCellNode {
  return node instanceof TableCellNode
}
