/**
 * Reader/editor chrome theme and typography (URL params + native preferences).
 */
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme } from "@/theme"
import {
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
} from "@/theme/reader-document-vars"
import type { ReaderPreferences } from "./editor-bridge-types"
import { clearSystemColorSchemeListener, ensureSystemColorSchemeListener } from "./editor-bridge-shared"

let platform: Platform = "compose"
let appearance: AppearanceMode = "auto"
let cursorColor: string | null = null

let readerPreferences: ReaderPreferences = {
  theme: "system",
  fontSize: "medium",
  lineHeight: "normal",
}

export function initAppearanceFromUrl(platformParam: Platform, appearanceParam: AppearanceMode): void {
  platform = platformParam
  appearance = appearanceParam
}

export function setBridgePlatform(p: Platform): void {
  platform = p
}

export function setBridgeAppearance(mode: AppearanceMode): void {
  appearance = mode
}

export function setBridgeCursorColor(color: string): void {
  cursorColor = color
}

export function mergeBridgeReaderPreferences(prefs: Partial<ReaderPreferences>): void {
  readerPreferences = {
    ...readerPreferences,
    ...prefs,
  }
}

export function applyReaderPreferences(): void {
  const page = document.body?.dataset.yabaPage
  const useReaderAppearancePipeline = page === "viewer" || page === "editor"

  if (useReaderAppearancePipeline) {
    if (readerPreferences.theme === "system") {
      applyTheme(platform, appearance, cursorColor)
      if (appearance === "auto") {
        ensureSystemColorSchemeListener(() => {
          if (readerPreferences.theme !== "system") return
          applyTheme(platform, appearance, cursorColor)
          applyReaderThemeCssVars(readerPreferences.theme)
        })
      } else {
        clearSystemColorSchemeListener()
      }
    } else if (readerPreferences.theme === "dark") {
      applyTheme(platform, "dark", cursorColor)
      clearSystemColorSchemeListener()
    } else if (readerPreferences.theme === "light") {
      applyTheme(platform, "light", cursorColor)
      clearSystemColorSchemeListener()
    } else {
      applyTheme(platform, "light", cursorColor)
      clearSystemColorSchemeListener()
    }
  } else {
    applyTheme(platform, appearance, cursorColor)
    clearSystemColorSchemeListener()
  }

  applyReaderThemeCssVars(readerPreferences.theme)
  applyReaderTypographyCssVars(readerPreferences)
}
