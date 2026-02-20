import type { Platform, AppearanceMode } from "./url-params"
import { composeLight, composeDark } from "./compose-palette"
import { darwinCssVars } from "./darwin-palette"

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

  if (platform === "compose") {
    const palette = dark ? composeDark : composeLight
    root.style.setProperty("--yaba-bg", palette.background)
    root.style.setProperty("--yaba-on-bg", palette.onBackground)
    root.style.setProperty("--yaba-surface", palette.surface)
    root.style.setProperty("--yaba-on-surface", palette.onSurface)
    root.style.setProperty("--yaba-surface-variant", palette.surfaceVariant)
    root.style.setProperty("--yaba-on-surface-variant", palette.onSurfaceVariant)
    root.style.setProperty("--yaba-outline", palette.outline)
    root.style.setProperty("--yaba-primary", palette.primary)
    root.style.setProperty("--yaba-on-primary", palette.onPrimary)
    root.style.setProperty("--yaba-primary-container", palette.primaryContainer)
    root.style.setProperty("--yaba-on-primary-container", palette.onPrimaryContainer)
  } else {
    const vars = dark ? darwinCssVars.dark : darwinCssVars.light
    root.style.setProperty("--yaba-bg", vars.background)
    root.style.setProperty("--yaba-on-bg", vars.onBackground)
    root.style.setProperty("--yaba-surface", vars.surface)
    root.style.setProperty("--yaba-on-surface", vars.onSurface)
    root.style.setProperty("--yaba-surface-variant", vars.surfaceVariant)
    root.style.setProperty("--yaba-on-surface-variant", vars.onSurfaceVariant)
    root.style.setProperty("--yaba-outline", vars.outline)
    root.style.setProperty("--yaba-primary", vars.primary)
    root.style.setProperty("--yaba-on-primary", vars.onPrimary)
    root.style.setProperty("--yaba-primary-container", vars.primaryContainer)
    root.style.setProperty("--yaba-on-primary-container", vars.onPrimaryContainer)
  }

  if (cursorColor) {
    root.style.setProperty("--yaba-cursor", cursorColor)
  } else {
    root.style.removeProperty("--yaba-cursor")
  }

  const fontStack =
    platform === "compose"
      ? '"Quicksand", -apple-system, BlinkMacSystemFont, sans-serif'
      : "-apple-system, BlinkMacSystemFont, system-ui, 'SF Pro Text', 'SF Pro Display', sans-serif"
  root.style.setProperty("--yaba-font-family", fontStack)
}
