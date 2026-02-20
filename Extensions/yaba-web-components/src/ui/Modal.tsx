import type { ReactNode } from "react"
import { useEffect, useRef } from "react"

function isDOMNode(target: unknown): target is Node {
  return target instanceof Node
}
import { createPortal } from "react-dom"
import "./Modal.css"

function PortalImpl({
  onClose,
  children,
  title,
  closeOnClickOutside,
}: {
  children: ReactNode
  closeOnClickOutside: boolean
  onClose: () => void
  title: string
}) {
  const modalRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (modalRef.current !== null) modalRef.current.focus()
  }, [])

  useEffect(() => {
    let modalOverlayElement: HTMLElement | null = null
    const handler = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose()
    }
    const clickOutsideHandler = (event: MouseEvent) => {
      const target = event.target
      if (
        modalRef.current !== null &&
        isDOMNode(target) &&
        !modalRef.current.contains(target) &&
        closeOnClickOutside
      ) {
        onClose()
      }
    }
    const modelElement = modalRef.current
    if (modelElement !== null) {
      modalOverlayElement = modelElement.parentElement
      if (modalOverlayElement !== null) {
        modalOverlayElement.addEventListener("click", clickOutsideHandler)
      }
    }
    window.addEventListener("keydown", handler)
    return () => {
      window.removeEventListener("keydown", handler)
      modalOverlayElement?.removeEventListener("click", clickOutsideHandler)
    }
  }, [closeOnClickOutside, onClose])

  return createPortal(
    <div className="Modal__overlay" role="dialog">
      <div
        className="Modal__modal"
        ref={modalRef}
        tabIndex={-1}
        aria-modal
        aria-labelledby="modal-title"
      >
        <h2 id="modal-title" className="Modal__title">
          {title}
        </h2>
        <button
          type="button"
          className="Modal__closeButton"
          onClick={onClose}
          aria-label="Close modal"
        >
          ×
        </button>
        <div className="Modal__content">{children}</div>
      </div>
    </div>,
    document.body
  )
}

export default function Modal({
  onClose,
  children,
  title,
  closeOnClickOutside = false,
}: {
  children: ReactNode
  closeOnClickOutside?: boolean
  onClose: () => void
  title: string
}) {
  return createPortal(
    <PortalImpl
      onClose={onClose}
      title={title}
      closeOnClickOutside={closeOnClickOutside}
    >
      {children}
    </PortalImpl>,
    document.body
  )
}
