import type { ExcalidrawInitialElements } from "@/ui/ExcalidrawModal"
import type { AppState, BinaryFiles } from "@excalidraw/excalidraw/types"
import type { NodeViewProps } from "@tiptap/core"
import { NodeViewWrapper } from "@tiptap/react"
import { useCallback, useMemo, useRef, useState } from "react"
import ExcalidrawModal from "@/ui/ExcalidrawModal"
import ExcalidrawImage from "./ExcalidrawImage"

export function ExcalidrawNodeView({ node, editor, deleteNode, updateAttributes }: NodeViewProps) {
  const data = node.attrs.data as string
  const isEditable = editor.isEditable
  const imageContainerRef = useRef<HTMLDivElement | null>(null)

  const { elements = [], files = {}, appState = {} } = useMemo(() => {
    if (!data || data === "{}") return {}
    try {
      return JSON.parse(data) as { elements?: ExcalidrawInitialElements; files?: BinaryFiles; appState?: AppState }
    } catch {
      return {}
    }
  }, [data])

  const isEmpty = !elements?.length && !Object.keys(files || {}).length
  const [isModalOpen, setModalOpen] = useState(isEmpty && isEditable)

  const setData = useCallback(
    (els: ExcalidrawInitialElements, aps: Partial<AppState>, fls: BinaryFiles) => {
      if ((els && els.length > 0) || Object.keys(fls).length > 0) {
        updateAttributes({
          data: JSON.stringify({
            appState: aps,
            elements: els,
            files: fls,
          }),
        })
      } else {
        deleteNode()
      }
    },
    [updateAttributes, deleteNode]
  )

  const handleDelete = useCallback(() => {
    setModalOpen(false)
    deleteNode()
  }, [deleteNode])

  const handleClose = useCallback(() => {
    setModalOpen(false)
    if (isEmpty) {
      deleteNode()
    }
  }, [deleteNode, isEmpty])

  const handleSave = useCallback(
    (els: ExcalidrawInitialElements, aps: Partial<AppState>, fls: BinaryFiles) => {
      setData(els, aps, fls)
      setModalOpen(false)
    },
    [setData]
  )

  const openModal = useCallback(() => setModalOpen(true), [])

  return (
    <NodeViewWrapper className="yaba-excalidraw-wrapper" as="div">
      {isEditable && isModalOpen && (
        <ExcalidrawModal
          initialElements={elements as ExcalidrawInitialElements}
          initialAppState={appState as AppState}
          initialFiles={(files || {}) as BinaryFiles}
          isShown={isModalOpen}
          onDelete={handleDelete}
          onClose={handleClose}
          onSave={handleSave}
          closeOnClickOutside={false}
        />
      )}
      {elements?.length ? (
        <div style={{ position: "relative", display: "block" }}>
          <ExcalidrawImage
            elements={elements}
            files={(files || {}) as BinaryFiles}
            imageContainerRef={imageContainerRef}
            appState={(appState || {}) as AppState}
            width="inherit"
            height="inherit"
          />
          {isEditable && (
            <button
              type="button"
              className="yaba-excalidraw-edit-button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={openModal}
            >
              Edit
            </button>
          )}
        </div>
      ) : null}
    </NodeViewWrapper>
  )
}
