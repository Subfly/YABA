import type { ReactNode } from "react"
import "./Button.css"

interface ButtonProps {
  "data-test-id"?: string
  children: ReactNode
  className?: string
  disabled?: boolean
  onClick: () => void
  small?: boolean
  title?: string
}

export default function Button({
  "data-test-id": dataTestId,
  children,
  className,
  disabled,
  onClick,
  small,
  title,
}: ButtonProps) {
  return (
    <button
      type="button"
      data-test-id={dataTestId}
      className={`Button__root${small ? " Button__small" : ""}${disabled ? " Button__disabled" : ""} ${className ?? ""}`.trim()}
      disabled={disabled}
      onClick={onClick}
      title={title}
    >
      {children}
    </button>
  )
}
