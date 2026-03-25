import type { AppearanceMode, Platform } from "./url-params"
import { applyTheme } from "./apply-theme"

/** Matches [ReaderPreferences] / Kotlin [toJsReader*Literal] values. */
export type ReaderThemeName = "system" | "dark" | "light" | "sepia"

export interface ReaderTypographyPrefs {
  fontSize: string
  lineHeight: string
}

const readerFontSizeCss: Record<string, string> = {
  small: "16px",
  medium: "18px",
  large: "22px",
}

const readerLineHeightCss: Record<string, string> = {
  normal: "1.6",
  relaxed: "1.8",
}

/** Same as [applyReaderThemeVars] in editor-bridge — sets reader surface colors on :root. */
export function applyReaderThemeCssVars(theme: ReaderThemeName): void {
  const root = document.documentElement

  if (theme === "system") {
    root.style.setProperty("--yaba-reader-bg", "transparent")
    root.style.setProperty("--yaba-reader-on-bg", "var(--yaba-on-bg)")
    return
  }

  if (theme === "dark" || theme === "light") {
    root.style.setProperty("--yaba-reader-bg", "var(--yaba-bg)")
    root.style.setProperty("--yaba-reader-on-bg", "var(--yaba-on-bg)")
    return
  }

  root.style.setProperty("--yaba-reader-bg", "#f4ecd8")
  root.style.setProperty("--yaba-reader-on-bg", "#5b4636")
}

export function applyReaderTypographyCssVars(prefs: ReaderTypographyPrefs): void {
  const root = document.documentElement
  const fs = prefs.fontSize in readerFontSizeCss ? prefs.fontSize : "medium"
  const lh = prefs.lineHeight in readerLineHeightCss ? prefs.lineHeight : "normal"
  root.style.setProperty("--yaba-reader-font-size", readerFontSizeCss[fs] ?? readerFontSizeCss.medium)
  root.style.setProperty("--yaba-reader-line-height", readerLineHeightCss[lh] ?? readerLineHeightCss.normal)
}

/**
 * Base shell + reader theme pipeline for embedded readers (viewer, editor, EPUB).
 * When [readerTheme] is system, [shellAppearance] drives light/dark via [applyTheme].
 */
export function applyBaseThemeForReaderTheme(
  platform: Platform,
  shellAppearance: AppearanceMode,
  readerTheme: ReaderThemeName,
  cursorColor: string | null,
): void {
  if (readerTheme === "system") {
    applyTheme(platform, shellAppearance, cursorColor)
  } else if (readerTheme === "dark") {
    applyTheme(platform, "dark", cursorColor)
  } else if (readerTheme === "light") {
    applyTheme(platform, "light", cursorColor)
  } else {
    applyTheme(platform, "light", cursorColor)
  }
}
