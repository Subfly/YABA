import type { Klass, LexicalNode } from "lexical"
import { HeadingNode, QuoteNode } from "@lexical/rich-text"
import { ListNode, ListItemNode } from "@lexical/list"
import { LinkNode, AutoLinkNode } from "@lexical/link"
import { CodeNode, CodeHighlightNode } from "@lexical/code"
import { TableNode, TableCellNode, TableRowNode } from "@lexical/table"
import { HorizontalRuleNode } from "@lexical/react/LexicalHorizontalRuleNode"
import { ImageNode } from "./nodes/ImageNode"
import { ExcalidrawNode } from "./nodes/ExcalidrawNode"

export const LEXICAL_NODES: ReadonlyArray<Klass<LexicalNode>> = [
  HeadingNode,
  QuoteNode,
  ListNode,
  ListItemNode,
  LinkNode,
  AutoLinkNode,
  CodeNode,
  CodeHighlightNode,
  TableNode,
  TableCellNode,
  TableRowNode,
  HorizontalRuleNode,
  ImageNode,
  ExcalidrawNode,
]
