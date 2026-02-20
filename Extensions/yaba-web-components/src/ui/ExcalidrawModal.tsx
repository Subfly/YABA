import type {
  AppState,
  BinaryFiles,
  ExcalidrawImperativeAPI,
  ExcalidrawInitialDataState,
} from "@excalidraw/excalidraw/types"
import type { ReactPortal } from "react"
import { Excalidraw } from "@excalidraw/excalidraw"
import { useEffect, useLayoutEffect, useRef, useState } from "react"

function isDOMNode(target: unknown): target is Node {
  return target instanceof Node
}
import { createPortal } from "react-dom"
import Button from "./Button"
import Modal from "./Modal"
import "./ExcalidrawModal.css"

export type ExcalidrawInitialElements = ExcalidrawInitialDataState["elements"]

interface ExcalidrawModalProps {
  closeOnClickOutside?: boolean
  initialElements: ExcalidrawInitialElements
  initialAppState: AppState
  initialFiles: BinaryFiles
  isShown?: boolean
  onClose: () => void
  onDelete: () => void
  onSave: (
    elements: ExcalidrawInitialElements,
    appState: Partial<AppState>,
    files: BinaryFiles
  ) => void
}

export default function ExcalidrawModal({
  closeOnClickOutside = false,
  onSave,
  initialElements,
  initialAppState,
  initialFiles,
  isShown = false,
  onDelete,
  onClose,
}: ExcalidrawModalProps): ReactPortal | null {
  const excaliDrawModelRef = useRef<HTMLDivElement | null>(null)
  const [excalidrawAPI, setExcalidrawAPI] =
    useState<ExcalidrawImperativeAPI | null>(null)
  const [discardModalOpen, setDiscardModalOpen] = useState(false)
  const [elements, setElements] =
    useState<ExcalidrawInitialElements>(initialElements)
  const [files, setFiles] = useState<BinaryFiles>(initialFiles)

  useEffect(() => {
    excaliDrawModelRef.current?.focus()
  }, [])

  useEffect(() => {
    let modalOverlayElement: HTMLElement | null = null

    const clickOutsideHandler = (event: MouseEvent) => {
      const target = event.target
      if (
        excaliDrawModelRef.current !== null &&
        isDOMNode(target) &&
        !excaliDrawModelRef.current.contains(target) &&
        closeOnClickOutside
      ) {
        onDelete()
      }
    }

    if (excaliDrawModelRef.current !== null) {
      modalOverlayElement = excaliDrawModelRef.current?.parentElement ?? null
      modalOverlayElement?.addEventListener("click", clickOutsideHandler)
    }

    return () => {
      modalOverlayElement?.removeEventListener("click", clickOutsideHandler)
    }
  }, [closeOnClickOutside, onDelete])

  useLayoutEffect(() => {
    const currentModalRef = excaliDrawModelRef.current

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onDelete()
    }

    currentModalRef?.addEventListener("keydown", onKeyDown)

    return () => {
      currentModalRef?.removeEventListener("keydown", onKeyDown)
    }
  }, [elements, files, onDelete])

  const save = () => {
    if (elements?.some((el) => !el.isDeleted)) {
      const appState = excalidrawAPI?.getAppState()
      const partialState: Partial<AppState> = {
        exportBackground: appState?.exportBackground,
        exportScale: appState?.exportScale,
        exportWithDarkMode: appState?.theme === "dark",
        isBindingEnabled: appState?.isBindingEnabled,
        isLoading: appState?.isLoading,
        name: appState?.name,
        theme: appState?.theme,
        viewBackgroundColor: appState?.viewBackgroundColor,
        viewModeEnabled: appState?.viewModeEnabled,
        zenModeEnabled: appState?.zenModeEnabled,
        zoom: appState?.zoom,
      }
      onSave(elements, partialState, files)
    } else {
      onDelete()
    }
  }

  const discard = () => {
    setDiscardModalOpen(true)
  }

  function ShowDiscardDialog() {
    return (
      <Modal
        title="Discard"
        onClose={() => {
          setDiscardModalOpen(false)
        }}
        closeOnClickOutside={false}
      >
        Are you sure you want to discard the changes?
        <div className="ExcalidrawModal__discardModal">
          <Button
            onClick={() => {
              setDiscardModalOpen(false)
              onClose()
            }}
          >
            Discard
          </Button>{" "}
          <Button
            onClick={() => {
              setDiscardModalOpen(false)
            }}
          >
            Cancel
          </Button>
        </div>
      </Modal>
    )
  }

  if (isShown === false) {
    return null
  }

  const onChange = (
    els: ExcalidrawInitialElements,
    _: AppState,
    fls: BinaryFiles
  ) => {
    setElements(els)
    setFiles(fls)
  }

  return createPortal(
    <div className="ExcalidrawModal__overlay" role="dialog">
      <div
        className="ExcalidrawModal__modal"
        ref={excaliDrawModelRef}
        tabIndex={-1}
      >
        <div className="ExcalidrawModal__row">
          {discardModalOpen && <ShowDiscardDialog />}
          <Excalidraw
            onChange={onChange}
            excalidrawAPI={setExcalidrawAPI}
            initialData={{
              appState: initialAppState || { isLoading: false },
              elements: initialElements,
              files: initialFiles,
            }}
          />
          <div className="ExcalidrawModal__actions">
            <button type="button" className="action-button" onClick={discard}>
              Discard
            </button>
            <button type="button" className="action-button" onClick={save}>
              Save
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  )
}
