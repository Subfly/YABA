import type { ExcalidrawInitialElements } from "@/ui/ExcalidrawModal"
import type { AppState, BinaryFiles } from "@excalidraw/excalidraw/types"
import type { NodeKey } from "lexical"
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext"
import { useLexicalEditable } from "@lexical/react/useLexicalEditable"
import { useLexicalNodeSelection } from "@lexical/react/useLexicalNodeSelection"
import { mergeRegister } from "@lexical/utils"
import { $getNodeByKey, CLICK_COMMAND, COMMAND_PRIORITY_LOW, isDOMNode } from "lexical"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import ExcalidrawModal from "@/ui/ExcalidrawModal"
import { $isExcalidrawNode } from "./index"
import ExcalidrawImage from "./ExcalidrawImage"

interface ExcalidrawComponentProps {
  nodeKey: NodeKey
  data: string
  width: "inherit" | number
  height: "inherit" | number
}

export default function ExcalidrawComponent({
  nodeKey,
  data,
  width,
  height,
}: ExcalidrawComponentProps) {
  const [editor] = useLexicalComposerContext()
  const isEditable = useLexicalEditable()
  const [isModalOpen, setModalOpen] = useState(
    data === "[]" && editor.isEditable()
  )
  const imageContainerRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const [isSelected, setSelected, clearSelection] =
    useLexicalNodeSelection(nodeKey)

  useEffect(() => {
    if (!isEditable) {
      if (isSelected) clearSelection()
      return
    }
    return mergeRegister(
      editor.registerCommand(
        CLICK_COMMAND,
        (event: MouseEvent) => {
          const buttonElem = buttonRef.current
          const eventTarget = event.target

          if (
            buttonElem !== null &&
            isDOMNode(eventTarget) &&
            buttonElem.contains(eventTarget)
          ) {
            if (!event.shiftKey) {
              clearSelection()
            }
            setSelected(!isSelected)
            if (event.detail > 1) {
              setModalOpen(true)
            }
            return true
          }

          return false
        },
        COMMAND_PRIORITY_LOW
      )
    )
  }, [clearSelection, editor, isSelected, setSelected, isEditable])

  const deleteNode = useCallback(() => {
    setModalOpen(false)
    return editor.update(() => {
      const node = $getNodeByKey(nodeKey)
      if (node) node.remove()
    })
  }, [editor, nodeKey])

  const setData = (
    els: ExcalidrawInitialElements,
    aps: Partial<AppState>,
    fls: BinaryFiles
  ) => {
    editor.update(() => {
      const node = $getNodeByKey(nodeKey)
      if ($isExcalidrawNode(node)) {
        if ((els && els.length > 0) || Object.keys(fls).length > 0) {
          node.setData(
            JSON.stringify({
              appState: aps,
              elements: els,
              files: fls,
            })
          )
        } else {
          node.remove()
        }
      }
    })
  }

  const openModal = useCallback(() => {
    setModalOpen(true)
  }, [])

  const { elements = [], files = {}, appState = {} } = useMemo(
    () => (data ? JSON.parse(data) : {}),
    [data]
  )

  const closeModal = useCallback(() => {
    setModalOpen(false)
    if (elements.length === 0) {
      editor.update(() => {
        const node = $getNodeByKey(nodeKey)
        if (node) node.remove()
      })
    }
  }, [editor, nodeKey, elements.length])

  return (
    <>
      {isEditable && isModalOpen && (
        <ExcalidrawModal
          initialElements={[]}
          initialAppState={{} as AppState}
          initialFiles={{}}
          isShown={isModalOpen}
          onDelete={deleteNode}
          onClose={closeModal}
          onSave={(els, aps, fls) => {
            setData(els, aps, fls)
            setModalOpen(false)
          }}
          closeOnClickOutside={false}
        />
      )}
      {elements.length > 0 && (
        <div
          className="yaba-excalidraw-wrapper"
          style={{ position: "relative", display: "block" }}
        >
          <ExcalidrawImage
            elements={elements}
            files={files}
            imageContainerRef={imageContainerRef}
            appState={appState}
            width={width}
            height={height}
          />
          {isSelected && isEditable && (
            <button
              ref={buttonRef}
              type="button"
              className="yaba-excalidraw-edit-button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={openModal}
            >
              Edit
            </button>
          )}
        </div>
      )}
    </>
  )
}
