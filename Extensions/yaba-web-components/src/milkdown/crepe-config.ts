/**
 * Declarative Crepe setup for YABA editor/viewer shells.
 * Aligns with @milkdown/crepe feature + featureConfigs patterns (see repo ./docs/api/crepe.md).
 */
import { Crepe, CrepeFeature } from "@milkdown/crepe"
import { rewriteAssetPathsInMarkdown } from "./markdown-assets"
import { getNoteEditorPlaceholderText } from "./note-placeholder"

export type CrepeShellVariant = "viewer" | "editor"

export interface CreateCrepeForShellArgs {
  root: HTMLElement
  variant: CrepeShellVariant
  editable: boolean
  /** Initial markdown (use [normalizeInitialMarkdownForCrepe] when loading with assets base). */
  initialMarkdown: string
}

/** Rewrite persisted `../assets/` URLs when a base URL is provided (viewer/editor load). */
export function normalizeInitialMarkdownForCrepe(initialMarkdown: string, assetsBaseUrl: string | undefined): string {
  let raw = initialMarkdown.trim() ? initialMarkdown : ""
  if (assetsBaseUrl && raw.includes("../assets/")) {
    raw = rewriteAssetPathsInMarkdown(raw, assetsBaseUrl)
  }
  return raw
}

/**
 * Builds a Crepe instance with YABA defaults: no Toolbar/TopBar, block placeholder, optional readonly.
 */
export function createCrepeForShell(args: CreateCrepeForShellArgs): Crepe {
  const defaultMd = args.initialMarkdown.trim() ? args.initialMarkdown : ""
  const placeholderText =
    args.variant === "editor" ? getNoteEditorPlaceholderText() || " " : " "

  const crepe = new Crepe({
    root: args.root,
    defaultValue: defaultMd || "",
    features: {
      [CrepeFeature.Toolbar]: false,
      [CrepeFeature.TopBar]: false,
    },
    featureConfigs: {
      [CrepeFeature.Placeholder]: {
        text: placeholderText,
        mode: "block",
      },
    },
  })

  if (!args.editable) {
    crepe.setReadonly(true)
  }

  return crepe
}
