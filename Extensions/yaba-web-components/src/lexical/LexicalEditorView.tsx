import { useEffect } from "react"
import { LexicalComposer } from "@lexical/react/LexicalComposer"
import { RichTextPlugin } from "@lexical/react/LexicalRichTextPlugin"
import { ContentEditable } from "@lexical/react/LexicalContentEditable"
import { HistoryPlugin } from "@lexical/react/LexicalHistoryPlugin"
import { ListPlugin } from "@lexical/react/LexicalListPlugin"
import { LinkPlugin } from "@lexical/react/LexicalLinkPlugin"
import { TabIndentationPlugin } from "@lexical/react/LexicalTabIndentationPlugin"
import { TablePlugin } from "@lexical/react/LexicalTablePlugin"
import { HorizontalRulePlugin } from "@lexical/react/LexicalHorizontalRulePlugin"
import { MarkdownShortcutPlugin } from "@lexical/react/LexicalMarkdownShortcutPlugin"
import ExcalidrawPlugin from "./plugins/ExcalidrawPlugin"
import { LexicalErrorBoundary } from "@lexical/react/LexicalErrorBoundary"
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext"
import { registerCodeHighlighting } from "@lexical/code"
import { $convertFromMarkdownString, TRANSFORMERS } from "@lexical/markdown"
import { LEXICAL_NODES } from "./editor-config"
import { IMAGE } from "./markdown-transformers"
import { TABLE } from "./table-transformer"
import { editorTheme } from "./editor-theme"
import "./editor-styles.css"

function CodeHighlightPlugin() {
  const [editor] = useLexicalComposerContext()
  useEffect(() => {
    return registerCodeHighlighting(editor)
  }, [editor])
  return null
}

interface LexicalEditorViewProps {
  editable: boolean
  initialMarkdown?: string
  onEditorReady?: (editor: import("lexical").LexicalEditor) => void
  assetsBaseUrl?: string
}

export function LexicalEditorView({
  editable,
  initialMarkdown,
  onEditorReady,
  assetsBaseUrl,
}: LexicalEditorViewProps) {
  const initialConfig = {
    namespace: "YabaEditor",
    theme: editorTheme,
    nodes: LEXICAL_NODES,
    editable,
    onError: (err: Error) => {
      console.error("Lexical error:", err)
    },
    ...(initialMarkdown != null &&
      initialMarkdown !== "" && {
        editorState: () => {
          let content = initialMarkdown
          if (assetsBaseUrl && content.includes("../assets/")) {
            const base = assetsBaseUrl.replace(/\/?$/, "/")
            content = content.replace(/\]\(\.\.\/assets\//g, `](${base}assets/`)
          }
          $convertFromMarkdownString(content, [...TRANSFORMERS, IMAGE, TABLE])
        },
      }),
  }

  return (
    <LexicalComposer initialConfig={initialConfig}>
      <LexicalEditorInner editable={editable} onEditorReady={onEditorReady} />
    </LexicalComposer>
  )
}

function LexicalEditorInner({
  editable,
  onEditorReady,
}: {
  editable: boolean
  onEditorReady?: (editor: import("lexical").LexicalEditor) => void
}) {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    onEditorReady?.(editor)
  }, [editor, onEditorReady])

  return (
    <div className="yaba-editor-container" data-yaba-editor-root>
      <RichTextPlugin
        contentEditable={
          <ContentEditable
            className="yaba-content-editable"
            style={{ caretColor: "var(--yaba-cursor, var(--yaba-primary))" }}
          />
        }
        placeholder={null}
        ErrorBoundary={LexicalErrorBoundary}
      />
      <HistoryPlugin />
      <ListPlugin />
      <LinkPlugin />
      <TabIndentationPlugin />
      <TablePlugin
        hasCellMerge={false}
        hasCellBackgroundColor={false}
        hasTabHandler={true}
      />
      <HorizontalRulePlugin />
      <ExcalidrawPlugin />
      {editable && <MarkdownShortcutPlugin />}
      <CodeHighlightPlugin />
    </div>
  )
}
