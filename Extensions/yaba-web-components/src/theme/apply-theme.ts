import type { Platform, AppearanceMode } from "./url-params"
import { yabaLight, yabaDark } from "./yaba-palette"

function isDark(appearance: AppearanceMode): boolean {
  if (appearance === "light") return false
  if (appearance === "dark") return true
  return window.matchMedia("(prefers-color-scheme: dark)").matches
}

export function applyTheme(
  platform: Platform,
  appearance: AppearanceMode,
  cursorColor: string | null
): void {
  const dark = isDark(appearance)
  const root = document.documentElement

  root.style.colorScheme = dark ? "dark" : "light"

  const palette = dark ? yabaDark : yabaLight

  if (platform === "android") {
    root.style.setProperty("--yaba-bg", palette.background)
    root.style.setProperty("--yaba-surface", palette.surface)
  } else {
    // Darwin (iOS WebView): same Material tokens as Android for content (links, code blocks, tables);
    // page background stays system `Canvas` so the native shell can show through. Font stack is set below.
    root.style.setProperty("--yaba-bg", "Canvas")
    root.style.setProperty("--yaba-surface", "Canvas")
  }

  root.style.setProperty("--yaba-on-bg", palette.onBackground)
  root.style.setProperty("--yaba-on-surface", palette.onSurface)
  root.style.setProperty("--yaba-surface-variant", palette.surfaceVariant)
  root.style.setProperty("--yaba-on-surface-variant", palette.onSurfaceVariant)
  root.style.setProperty("--yaba-outline", palette.outline)
  root.style.setProperty("--yaba-primary", palette.primary)
  root.style.setProperty("--yaba-on-primary", palette.onPrimary)
  root.style.setProperty("--yaba-primary-container", palette.primaryContainer)
  root.style.setProperty("--yaba-on-primary-container", palette.onPrimaryContainer)

  if (cursorColor) {
    root.style.setProperty("--yaba-cursor", cursorColor)
  } else {
    root.style.removeProperty("--yaba-cursor")
  }

  const fontStack =
    platform === "android"
      ? '"Quicksand", -apple-system, BlinkMacSystemFont, sans-serif'
      : "-apple-system, BlinkMacSystemFont, system-ui, 'SF Pro Text', 'SF Pro Display', sans-serif"
  root.style.setProperty("--yaba-font-family", fontStack)
}
