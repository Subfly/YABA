import { exportToBlob, exportToSvg, FONT_FAMILY, newElementWith, ROUNDNESS } from "@excalidraw/excalidraw"

/**
 * Values match `@excalidraw/excalidraw` `constants` (not re-exported from main entry; deep import breaks Vite).
 */
const ROUGHNESS = { architect: 0, artist: 1, cartoonist: 2 } as const
const STROKE_WIDTH = { thin: 1, bold: 2, extraBold: 4 } as const
const TEXT_ALIGN = { LEFT: "left", CENTER: "center", RIGHT: "right" } as const
const VERTICAL_ALIGN = { TOP: "top", MIDDLE: "middle", BOTTOM: "bottom" } as const
import type {
  ExcalidrawImperativeAPI,
  ExcalidrawInitialDataState,
} from "@excalidraw/excalidraw/types"
import type { YabaNativeHostPayload } from "./contracts/native-host"
import { postToYabaNativeHost } from "./yaba-native-host"
import {
  applyCanvasInlinePayload,
  getCanvasSelectionLinkContext,
  type ApplyCanvasInlinePayload,
} from "./canvas-inline"

/** Excalidraw's undo/redo use CTRL_OR_CMD + Z (and redo also Ctrl+Shift+Z / Ctrl+Y). */
function dispatchSyntheticUndoRedo(kind: "undo" | "redo"): void {
  const root =
    (document.querySelector(".excalidraw.excalidraw-container") as HTMLElement | null) ??
    (document.querySelector(".excalidraw-container") as HTMLElement | null) ??
    (document.querySelector(".excalidraw") as HTMLElement | null)
  if (root && root.getAttribute("tabindex") == null) {
    root.setAttribute("tabindex", "-1")
  }
  root?.focus()

  const targets: EventTarget[] = []
  if (root) targets.push(root)
  targets.push(document, window, document.body)
  const canvas = document.querySelector(".excalidraw__canvas")
  if (canvas) targets.push(canvas)

  const make = (init: KeyboardEventInit) =>
    new KeyboardEvent("keydown", {
      bubbles: true,
      cancelable: true,
      view: window,
      ...init,
    })

  const dispatches: KeyboardEventInit[] =
    kind === "undo"
      ? [
          { key: "z", code: "KeyZ", ctrlKey: true },
          { key: "z", code: "KeyZ", metaKey: true },
        ]
      : [
          { key: "z", code: "KeyZ", ctrlKey: true, shiftKey: true },
          { key: "z", code: "KeyZ", metaKey: true, shiftKey: true },
          { key: "y", code: "KeyY", ctrlKey: true },
        ]

  for (const init of dispatches) {
    for (const t of targets) {
      t.dispatchEvent(make(init))
    }
  }
}

function executeUndoRedo(kind: "undo" | "redo"): void {
  const api = excalidrawApi as ExcalidrawImperativeAPI & {
    actionManager?: {
      actions: Record<string, { name: string }>
      executeAction: (action: { name: string }, source?: string) => void
    }
  }
  const am = api?.actionManager
  const action = am?.actions?.[kind]
  if (am?.executeAction && action) {
    try {
      am.executeAction(action, "api")
      return
    } catch {
      /* fall through to keyboard */
    }
  }
  dispatchSyntheticUndoRedo(kind)
}

function executeNamedAction(name: "sendToBack" | "sendBackward" | "bringForward" | "bringToFront"): void {
  const api = excalidrawApi as ExcalidrawImperativeAPI & {
    actionManager?: {
      actions: Record<string, { name: string }>
      executeAction: (action: { name: string }, source?: string) => void
    }
  }
  const am = api?.actionManager
  const action = am?.actions?.[name]
  if (am?.executeAction && action) {
    try {
      am.executeAction(action, "api")
    } catch {
      /* ignore */
    }
  }
}

export type CanvasTool =
  | "selection"
  | "hand"
  | "draw"
  | "eraser"
  | "line"
  | "arrow"
  | "text"
  | "frame"
  | "rectangle"
  | "diamond"
  | "ellipse"

type HostCanvasMetrics = {
  type: "canvasMetrics"
  activeTool: CanvasTool
  hasSelection: boolean
  canUndo: boolean
  canRedo: boolean
  gridModeEnabled: boolean
  objectsSnapModeEnabled: boolean
}

/** Mirrors [YabaColor] ARGB RGB channels (Compose). NONE = transparent */
const YABA_PALETTE: { code: number; hex: string }[] = [
  { code: 1, hex: "#0088ff" },
  { code: 2, hex: "#ac7f5e" },
  { code: 3, hex: "#00c0e8" },
  { code: 4, hex: "#8e8e93" },
  { code: 5, hex: "#34c759" },
  { code: 6, hex: "#6155f5" },
  { code: 7, hex: "#00c8b3" },
  { code: 8, hex: "#ff8d28" },
  { code: 9, hex: "#ff2d55" },
  { code: 10, hex: "#cb30e0" },
  { code: 11, hex: "#ff383c" },
  { code: 12, hex: "#00c3d0" },
  { code: 13, hex: "#ffcc00" },
]

const FONT_SIZE_PRESETS = { S: 16, M: 20, L: 28, XL: 36 } as const

type StrokeWidthKey = "thin" | "bold" | "extraBold"
type RoughnessKey = "architect" | "artist" | "cartoonist"
type FontSizeKey = "S" | "M" | "L" | "XL"

export type ApplySelectionStylePayload = {
  strokeYabaCode?: number
  backgroundYabaCode?: number
  strokeWidthKey?: StrokeWidthKey
  strokeStyle?: "solid" | "dashed" | "dotted"
  roughnessKey?: RoughnessKey
  edgeKey?: "sharp" | "round"
  fontSizeKey?: FontSizeKey
  /** 0–10 */
  opacityStep?: number
  /** Linear (`line` / `arrow`) routing */
  arrowTypeKey?: "sharp" | "curved" | "elbow"
  /** Excalidraw arrowhead string, or `"none"` for null */
  startArrowheadKey?: string
  endArrowheadKey?: string
  /** Excalidraw `fillStyle` for filled shapes */
  fillStyleKey?: "solid" | "hachure" | "cross-hatch" | "zigzag"
}

/** Excalidraw `Arrowhead` union + `"none"` for UI / JSON transport */
export const EXCALI_ARROWHEAD_PICKER_ORDER: readonly string[] = [
  "none",
  "arrow",
  "triangle",
  "triangle_outline",
  "dot",
  "circle",
  "circle_outline",
  "diamond",
  "diamond_outline",
  "bar",
  "crowfoot_one",
  "crowfoot_many",
  "crowfoot_one_or_many",
] as const

type FillStyleKey = "solid" | "hachure" | "cross-hatch" | "zigzag"

function fillStyleToKey(raw: string | undefined): FillStyleKey {
  const s = String(raw ?? "solid")
  if (s === "hachure" || s === "cross-hatch" || s === "zigzag" || s === "solid") return s
  return "solid"
}

const OPTION_GROUP = {
  stroke: "stroke",
  background: "background",
  fillType: "fillType",
  strokeWidth: "strokeWidth",
  strokeStyle: "strokeStyle",
  sloppiness: "sloppiness",
  edges: "edges",
  fontSize: "fontSize",
  opacity: "opacity",
  layers: "layers",
  delete: "delete",
  arrowType: "arrowType",
  startArrowhead: "startArrowhead",
  endArrowhead: "endArrowhead",
} as const

const OPTION_GROUP_SORT_ORDER: string[] = [
  OPTION_GROUP.stroke,
  OPTION_GROUP.background,
  OPTION_GROUP.fillType,
  OPTION_GROUP.strokeWidth,
  OPTION_GROUP.strokeStyle,
  OPTION_GROUP.sloppiness,
  OPTION_GROUP.edges,
  OPTION_GROUP.fontSize,
  OPTION_GROUP.arrowType,
  OPTION_GROUP.startArrowhead,
  OPTION_GROUP.endArrowhead,
  OPTION_GROUP.opacity,
  OPTION_GROUP.layers,
  OPTION_GROUP.delete,
]

function sortOptionGroups(groups: Set<string>): string[] {
  const arr = [...groups]
  arr.sort((a, b) => {
    const ia = OPTION_GROUP_SORT_ORDER.indexOf(a)
    const ib = OPTION_GROUP_SORT_ORDER.indexOf(b)
    const va = ia === -1 ? 999 : ia
    const vb = ib === -1 ? 999 : ib
    if (va !== vb) return va - vb
    return a.localeCompare(b)
  })
  return arr
}

function optionGroupsForElement(el: { type: string }): Set<string> {
  const t = el.type
  const common = new Set<string>([
    OPTION_GROUP.opacity,
    OPTION_GROUP.layers,
    OPTION_GROUP.delete,
  ])
  if (t === "text") {
    ;[OPTION_GROUP.stroke, OPTION_GROUP.fontSize].forEach((g) => common.add(g))
    return common
  }
  if (t === "frame") {
    return common
  }
  if (t === "rectangle" || t === "diamond" || t === "ellipse") {
    ;[
      OPTION_GROUP.stroke,
      OPTION_GROUP.background,
      OPTION_GROUP.fillType,
      OPTION_GROUP.strokeWidth,
      OPTION_GROUP.strokeStyle,
      OPTION_GROUP.sloppiness,
      OPTION_GROUP.edges,
    ].forEach((g) => common.add(g))
    return common
  }
  if (t === "line" || t === "arrow") {
    ;[
      OPTION_GROUP.stroke,
      OPTION_GROUP.strokeWidth,
      OPTION_GROUP.strokeStyle,
      OPTION_GROUP.sloppiness,
      OPTION_GROUP.arrowType,
      OPTION_GROUP.startArrowhead,
      OPTION_GROUP.endArrowhead,
    ].forEach((g) => common.add(g))
    return common
  }
  // image, freedraw, generic
  ;[OPTION_GROUP.stroke, OPTION_GROUP.background, OPTION_GROUP.fillType].forEach((g) => common.add(g))
  return common
}

function intersectOptionGroups(selected: { type: string }[]): string[] {
  if (selected.length === 0) return []
  let acc = optionGroupsForElement(selected[0])
  for (let i = 1; i < selected.length; i++) {
    const next = optionGroupsForElement(selected[i])
    acc = new Set([...acc].filter((g) => next.has(g)))
  }
  return sortOptionGroups(acc)
}

function arrowheadToKey(raw: string | null | undefined): string {
  if (raw == null || raw === "") return "none"
  return String(raw)
}

function keyToArrowhead(k: string | undefined): string | null {
  if (k === undefined) return null
  if (k === "none" || k === "") return null
  return k
}

type ArrowTypeKey = "sharp" | "curved" | "elbow"

function getArrowTypeKey(el: any): ArrowTypeKey {
  if (el.type !== "line" && el.type !== "arrow") return "sharp"
  if (el.type === "arrow" && el.elbowed === true) return "elbow"
  if (el.roundness != null) return "curved"
  return "sharp"
}

function parseHexRgb(hex: string): { r: number; g: number; b: number } | null {
  const h = hex.trim().replace(/^#/, "")
  if (h.length === 3) {
    const r = parseInt(h[0] + h[0], 16)
    const g = parseInt(h[1] + h[1], 16)
    const b = parseInt(h[2] + h[2], 16)
    return { r, g, b }
  }
  if (h.length === 6 || h.length === 8) {
    return {
      r: parseInt(h.slice(0, 2), 16),
      g: parseInt(h.slice(2, 4), 16),
      b: parseInt(h.slice(4, 6), 16),
    }
  }
  return null
}

function colorDistance(a: { r: number; g: number; b: number }, b: { r: number; g: number; b: number }): number {
  const dr = a.r - b.r
  const dg = a.g - b.g
  const db = a.b - b.b
  return dr * dr + dg * dg + db * db
}

function yabaCodeToHex(code: number): string | null {
  const row = YABA_PALETTE.find((p) => p.code === code)
  return row?.hex ?? null
}

function excalidrawColorToYabaCode(raw: string): number {
  const lower = raw.trim().toLowerCase()
  if (lower === "transparent" || lower === "") return 0
  const rgb = parseHexRgb(lower.startsWith("#") ? lower : `#${lower}`)
  if (!rgb) return 0
  let best = YABA_PALETTE[0]
  let bestD = colorDistance(rgb, parseHexRgb(best.hex)!)
  for (const p of YABA_PALETTE) {
    const pr = parseHexRgb(p.hex)!
    const d = colorDistance(rgb, pr)
    if (d < bestD) {
      bestD = d
      best = p
    }
  }
  return best.code
}

function strokeWidthToKey(w: number): StrokeWidthKey {
  const t = STROKE_WIDTH.thin
  const b = STROKE_WIDTH.bold
  const e = STROKE_WIDTH.extraBold
  const dist = (x: number) => Math.abs(w - x)
  if (dist(t) <= dist(b) && dist(t) <= dist(e)) return "thin"
  if (dist(b) <= dist(e)) return "bold"
  return "extraBold"
}

function roughnessToKey(r: number): RoughnessKey {
  if (r <= 0) return "architect"
  if (r === 1) return "artist"
  return "cartoonist"
}

function fontSizeToKey(size: number): FontSizeKey {
  const entries = Object.entries(FONT_SIZE_PRESETS) as [FontSizeKey, number][]
  let best: FontSizeKey = "M"
  let bd = Infinity
  for (const [k, v] of entries) {
    const d = Math.abs(size - v)
    if (d < bd) {
      bd = d
      best = k
    }
  }
  return best
}

function elementSupportsRoundness(el: { type: string }): boolean {
  return el.type === "rectangle" || el.type === "diamond" || el.type === "ellipse"
}

function getEdgeKey(el: { roundness?: unknown }): "sharp" | "round" {
  if (el.roundness == null) return "sharp"
  return "round"
}

function bytesToBase64(bytes: Uint8Array): string {
  let binary = ""
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize))
  }
  return btoa(binary)
}

async function blobToBase64(blob: Blob): Promise<string> {
  const buf = await blob.arrayBuffer()
  return bytesToBase64(new Uint8Array(buf))
}

/** Must match the scale we expose in the app (e.g. Excalidraw “3x” export). */
const YABA_EXPORT_SCALE = 3

function buildCanvasExportAppState(exportBackground: boolean): Record<string, unknown> {
  const api = excalidrawApi!
  const appState = api.getAppState() as Record<string, unknown>
  return {
    ...appState,
    exportBackground,
    exportWithDarkMode: false,
    exportEmbedScene: false,
    exportScale: YABA_EXPORT_SCALE,
  }
}

async function yabaExportCanvasImage(format: "png" | "svg", exportBackground: boolean): Promise<string> {
  const api = excalidrawApi
  if (!api) return JSON.stringify({ ok: false, error: "no_api" })
  const elements = api.getSceneElements()
  const files = api.getFiles()
  const exportAppState = buildCanvasExportAppState(exportBackground)

  try {
    if (format === "png") {
      /**
       * `@excalidraw/utils` `exportToBlob` → `exportToCanvas` does **not** apply
       * `appState.exportScale` unless `maxWidthOrHeight` is set. Otherwise it uses
       * `getDimensions?.(w,h) ?? { width, height }` with `scale: ret.scale ?? 1`.
       * So merging `exportScale: 3` into appState alone yields a 1× bitmap — blurry on device.
       */
      const blob = await exportToBlob({
        elements,
        files,
        appState: exportAppState,
        mimeType: "image/png",
        exportPadding: 10,
        getDimensions: (width: number, height: number) => ({
          width: width * YABA_EXPORT_SCALE,
          height: height * YABA_EXPORT_SCALE,
          scale: YABA_EXPORT_SCALE,
        }),
      })
      const base64 = await blobToBase64(blob)
      return JSON.stringify({ ok: true, base64 })
    }
    const svg = await exportToSvg({
      elements,
      files,
      appState: exportAppState,
      exportPadding: 10,
    })
    const svgString = svg.outerHTML
    const utf8 = new TextEncoder().encode(svgString)
    const base64 = bytesToBase64(utf8)
    return JSON.stringify({ ok: true, base64 })
  } catch (e) {
    return JSON.stringify({ ok: false, error: String(e) })
  }
}

const CANVAS_AUTOSAVE_IDLE_MS = 1000

let excalidrawApi: ExcalidrawImperativeAPI | null = null
let autosaveTimer: ReturnType<typeof setTimeout> | null = null
let lastMetricsJson: string | null = null
let lastStyleJson: string | null = null
let shellLoadPublished = false

const DEFAULT_SCENE: ExcalidrawInitialDataState = {
  elements: [],
  appState: {
    viewBackgroundColor: "#ffffff",
    currentItemFontFamily: FONT_FAMILY.Excalifont,
    currentItemTextAlign: TEXT_ALIGN.CENTER,
    gridModeEnabled: true,
    objectsSnapModeEnabled: true,
  },
  files: {},
}

function clearAutosaveTimer(): void {
  if (autosaveTimer !== null) {
    clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
}

function scheduleAutosaveIdle(): void {
  clearAutosaveTimer()
  autosaveTimer = setTimeout(() => {
    autosaveTimer = null
    postToYabaNativeHost({ type: "canvasAutosaveIdle" })
  }, CANVAS_AUTOSAVE_IDLE_MS)
}

function normalizeCanvasTool(toolType: string): CanvasTool {
  switch (toolType) {
    case "selection":
    case "hand":
    case "eraser":
    case "line":
    case "arrow":
    case "text":
    case "frame":
      return toolType
    case "rectangle":
    case "diamond":
    case "ellipse":
      return toolType
    case "freedraw":
      return "draw"
    default:
      return "selection"
  }
}

function getHistoryAvailability(): { canUndo: boolean; canRedo: boolean } {
  const api = excalidrawApi as ExcalidrawImperativeAPI & {
    actionManager?: {
      actions: { undo?: { name: string }; redo?: { name: string } }
      isActionEnabled: (action: { name: string }) => boolean
    }
  }
  const am = api?.actionManager
  const undo = am?.actions?.undo
  const redo = am?.actions?.redo
  if (am?.isActionEnabled && undo && redo) {
    try {
      return {
        canUndo: am.isActionEnabled(undo),
        canRedo: am.isActionEnabled(redo),
      }
    } catch {
      /* fall through */
    }
  }
  return { canUndo: true, canRedo: true }
}

function publishMetrics(): void {
  if (!excalidrawApi) return
  const appState = excalidrawApi.getAppState()
  const elements = excalidrawApi.getSceneElements()
  const hasSelection = elements.some((el) => appState.selectedElementIds[el.id])
  const { canUndo, canRedo } = getHistoryAvailability()
  const payload: HostCanvasMetrics = {
    type: "canvasMetrics",
    activeTool: normalizeCanvasTool(appState.activeTool?.type ?? "selection"),
    hasSelection,
    canUndo,
    canRedo,
    gridModeEnabled: appState.gridModeEnabled ?? false,
    objectsSnapModeEnabled: appState.objectsSnapModeEnabled ?? false,
  }
  const json = JSON.stringify(payload)
  if (json === lastMetricsJson) return
  lastMetricsJson = json
  postToYabaNativeHost(payload)
}

function publishStyleState(): void {
  if (!excalidrawApi) return
  const appState = excalidrawApi.getAppState()
  const elements = excalidrawApi.getSceneElements()
  const selected = elements.filter((el) => appState.selectedElementIds[el.id] && !el.isDeleted)
  const n = selected.length

  const arrowLists = [...EXCALI_ARROWHEAD_PICKER_ORDER]

  if (n === 0) {
    const empty: YabaNativeHostPayload = {
      type: "canvasStyleState",
      hasSelection: false,
      selectionCount: 0,
      selectionElementTypes: [],
      primaryElementType: "",
      elementTypeMixed: false,
      availableOptionGroups: [],
      strokeYabaCode: 0,
      backgroundYabaCode: 0,
      strokeWidthKey: "thin",
      strokeStyle: "solid",
      roughnessKey: "architect",
      edgeKey: "sharp",
      fontSizeKey: "M",
      opacityStep: 10,
      mixedStroke: false,
      mixedBackground: false,
      mixedStrokeWidth: false,
      mixedStrokeStyle: false,
      mixedRoughness: false,
      mixedEdge: false,
      mixedFontSize: false,
      mixedOpacity: false,
      arrowTypeKey: "sharp",
      mixedArrowType: false,
      startArrowheadKey: "none",
      endArrowheadKey: "none",
      mixedStartArrowhead: false,
      mixedEndArrowhead: false,
      availableStartArrowheads: arrowLists,
      availableEndArrowheads: arrowLists,
      fillStyleKey: "solid",
      mixedFillStyle: false,
    }
    const json = JSON.stringify(empty)
    if (json === lastStyleJson) return
    lastStyleJson = json
    postToYabaNativeHost(empty)
    return
  }

  const first = selected[0] as any
  const strokeYabaCode = excalidrawColorToYabaCode(String(first.strokeColor ?? "#000000"))
  const bgRaw = String(first.backgroundColor ?? "transparent")
  const backgroundYabaCode =
    bgRaw.toLowerCase() === "transparent" ? 0 : excalidrawColorToYabaCode(bgRaw)

  const strokeWidthKey = strokeWidthToKey(Number(first.strokeWidth ?? STROKE_WIDTH.thin))
  const strokeStyle = (first.strokeStyle ?? "solid") as "solid" | "dashed" | "dotted"
  const roughnessKey = roughnessToKey(Number(first.roughness ?? 0))
  const opacityStep = Math.max(0, Math.min(10, Math.round(Number(first.opacity ?? 100) / 10)))

  let edgeKey: "sharp" | "round" = "sharp"
  if (elementSupportsRoundness(first)) {
    edgeKey = getEdgeKey(first)
  }

  let fontSizeKey: FontSizeKey = "M"
  if (first.type === "text") {
    fontSizeKey = fontSizeToKey(Number(first.fontSize ?? FONT_SIZE_PRESETS.M))
  }

  const bgToCode = (raw: string) =>
    raw.toLowerCase() === "transparent" ? 0 : excalidrawColorToYabaCode(raw)
  const mixedStroke = selected.some(
    (el: any) => excalidrawColorToYabaCode(String(el.strokeColor)) !== strokeYabaCode
  )
  const fbBg = String(first.backgroundColor ?? "transparent")
  const mixedBackground = selected.some(
    (el: any) => bgToCode(String(el.backgroundColor ?? "transparent")) !== bgToCode(fbBg)
  )
  const fillStyleKey = fillStyleToKey(String(first.fillStyle ?? "solid"))
  const mixedFillStyle = selected.some(
    (el: any) => fillStyleToKey(String(el.fillStyle ?? "solid")) !== fillStyleKey
  )
  const mixedStrokeWidth = selected.some(
    (el: any) => strokeWidthToKey(Number(el.strokeWidth)) !== strokeWidthKey
  )
  const mixedStrokeStyle = selected.some((el: any) => (el.strokeStyle ?? "solid") !== strokeStyle)
  const mixedRoughness = selected.some((el: any) => roughnessToKey(Number(el.roughness)) !== roughnessKey)
  const mixedOpacity = selected.some(
    (el: any) => Math.round(Number(el.opacity ?? 100) / 10) !== opacityStep
  )
  const mixedEdge = selected.some((el: any) => {
    if (!elementSupportsRoundness(el)) return false
    return getEdgeKey(el) !== edgeKey
  })
  const mixedFontSize = selected.some((el: any) => {
    if (el.type !== "text" || first.type !== "text") return el.type !== first.type
    return fontSizeToKey(Number(el.fontSize ?? FONT_SIZE_PRESETS.M)) !== fontSizeKey
  })

  const selectionElementTypes = [...new Set(selected.map((e: any) => String(e.type)))]
  const primaryElementType = String(first.type)
  const elementTypeMixed = selectionElementTypes.length > 1
  const availableOptionGroups = intersectOptionGroups(selected as { type: string }[])

  const linearSelected = selected.filter((e: any) => e.type === "line" || e.type === "arrow")
  let arrowTypeKey: ArrowTypeKey = "sharp"
  let mixedArrowType = false
  let startArrowheadKey = "none"
  let endArrowheadKey = "none"
  let mixedStartArrowhead = false
  let mixedEndArrowhead = false
  if (linearSelected.length > 0) {
    const lf = linearSelected[0] as any
    arrowTypeKey = getArrowTypeKey(lf)
    startArrowheadKey = arrowheadToKey(lf.startArrowhead)
    endArrowheadKey = arrowheadToKey(lf.endArrowhead)
    mixedArrowType = linearSelected.some((e: any) => getArrowTypeKey(e) !== arrowTypeKey)
    mixedStartArrowhead = linearSelected.some(
      (e: any) => arrowheadToKey(e.startArrowhead) !== startArrowheadKey
    )
    mixedEndArrowhead = linearSelected.some((e: any) => arrowheadToKey(e.endArrowhead) !== endArrowheadKey)
  }

  const payload: YabaNativeHostPayload = {
    type: "canvasStyleState",
    hasSelection: true,
    selectionCount: n,
    selectionElementTypes,
    primaryElementType,
    elementTypeMixed,
    availableOptionGroups,
    strokeYabaCode,
    backgroundYabaCode,
    strokeWidthKey,
    strokeStyle,
    roughnessKey,
    edgeKey,
    fontSizeKey,
    opacityStep,
    mixedStroke,
    mixedBackground,
    mixedStrokeWidth,
    mixedStrokeStyle,
    mixedRoughness,
    mixedEdge,
    mixedFontSize,
    mixedOpacity,
    arrowTypeKey,
    mixedArrowType,
    startArrowheadKey,
    endArrowheadKey,
    mixedStartArrowhead,
    mixedEndArrowhead,
    availableStartArrowheads: arrowLists,
    availableEndArrowheads: arrowLists,
    fillStyleKey,
    mixedFillStyle,
  }

  const json = JSON.stringify(payload)
  if (json === lastStyleJson) return
  lastStyleJson = json
  postToYabaNativeHost(payload)
}

function publishCanvasHostState(): void {
  publishMetrics()
  publishStyleState()
}

function toExcalidrawTool(tool: CanvasTool): ExcalidrawImperativeAPI["setActiveTool"] extends (
  arg: infer T
) => void
  ? T
  : never {
  switch (tool) {
    case "draw":
      return { type: "freedraw", locked: false }
    case "rectangle":
    case "diamond":
    case "ellipse":
      return { type: tool, locked: false }
    default:
      return { type: tool, locked: false }
  }
}

function makeImageElementFromDataUrl(dataUrl: string): void {
  if (!excalidrawApi) return
  const fileId = `img_${Date.now()}`
  const appState = excalidrawApi.getAppState()
  const viewportCenterX = appState.scrollX * -1 + appState.width / 2
  const viewportCenterY = appState.scrollY * -1 + appState.height / 2
  const imageElement = {
    id: `el_${Date.now()}`,
    type: "image" as const,
    x: viewportCenterX - 150,
    y: viewportCenterY - 100,
    width: 300,
    height: 200,
    angle: 0,
    strokeColor: "transparent",
    backgroundColor: "transparent",
    fillStyle: "solid" as const,
    strokeWidth: 1,
    strokeStyle: "solid" as const,
    roughness: 0,
    opacity: 100,
    groupIds: [],
    frameId: null,
    roundness: null,
    seed: Math.floor(Math.random() * 1_000_000),
    version: 1,
    versionNonce: Math.floor(Math.random() * 1_000_000),
    isDeleted: false,
    boundElements: null,
    updated: Date.now(),
    link: null,
    locked: false,
    fileId,
    status: "saved" as const,
    scale: [1, 1] as [number, number],
    crop: null,
  }
  excalidrawApi.addFiles(
    [
      {
        id: fileId,
        dataURL: dataUrl,
        mimeType: dataUrl.substring(5, dataUrl.indexOf(";")) || "image/png",
        created: Date.now(),
        lastRetrieved: Date.now(),
      },
    ] as any
  )
  excalidrawApi.updateScene({
    elements: [...excalidrawApi.getSceneElements(), imageElement as any],
  })
}

function applySelectionStylePayload(partial: ApplySelectionStylePayload): void {
  const api = excalidrawApi
  if (!api) return
  const appState = api.getAppState()
  const selectedIds = appState.selectedElementIds
  const elements = api.getSceneElements()
  const roughnessFromKey = (k: RoughnessKey) =>
    k === "architect" ? ROUGHNESS.architect : k === "artist" ? ROUGHNESS.artist : ROUGHNESS.cartoonist

  const next = elements.map((el) => {
    if (!selectedIds[el.id]) return el
    const cur = el as any
    let updates: Record<string, unknown> = {}

    if (partial.strokeYabaCode !== undefined && partial.strokeYabaCode > 0) {
      const hx = yabaCodeToHex(partial.strokeYabaCode)
      if (hx) updates.strokeColor = hx
    }
    if (partial.backgroundYabaCode !== undefined) {
      if (partial.backgroundYabaCode === 0) {
        updates.backgroundColor = "transparent"
      } else {
        const hx = yabaCodeToHex(partial.backgroundYabaCode)
        if (hx) updates.backgroundColor = hx
      }
    }
    if (partial.fillStyleKey !== undefined) {
      updates.fillStyle = partial.fillStyleKey
    }
    if (partial.strokeWidthKey !== undefined) {
      const sw =
        partial.strokeWidthKey === "thin"
          ? STROKE_WIDTH.thin
          : partial.strokeWidthKey === "bold"
            ? STROKE_WIDTH.bold
            : STROKE_WIDTH.extraBold
      updates.strokeWidth = sw
    }
    if (partial.strokeStyle !== undefined) {
      updates.strokeStyle = partial.strokeStyle
    }
    if (partial.roughnessKey !== undefined) {
      updates.roughness = roughnessFromKey(partial.roughnessKey)
    }
    if (partial.opacityStep !== undefined) {
      updates.opacity = Math.max(0, Math.min(100, partial.opacityStep * 10))
    }
    if (partial.edgeKey !== undefined && elementSupportsRoundness(cur)) {
      updates.roundness =
        partial.edgeKey === "round" ? { type: ROUNDNESS.PROPORTIONAL_RADIUS } : null
    }
    if (cur.type === "text") {
      updates.fontFamily = FONT_FAMILY.Excalifont
      updates.textAlign = TEXT_ALIGN.CENTER
      updates.verticalAlign = VERTICAL_ALIGN.MIDDLE
      if (partial.fontSizeKey !== undefined) {
        updates.fontSize = FONT_SIZE_PRESETS[partial.fontSizeKey]
      }
    }

    if ((cur.type === "line" || cur.type === "arrow") && partial.arrowTypeKey !== undefined) {
      const k = partial.arrowTypeKey
      if (k === "elbow") {
        updates.type = "arrow"
        updates.elbowed = true
        updates.roundness = null
      } else if (k === "curved") {
        if (cur.type === "arrow") {
          updates.elbowed = false
        }
        updates.roundness = { type: ROUNDNESS.ADAPTIVE_RADIUS }
      } else {
        if (cur.type === "arrow") {
          updates.elbowed = false
        }
        updates.roundness = null
      }
    }
    if ((cur.type === "line" || cur.type === "arrow") && partial.startArrowheadKey !== undefined) {
      updates.startArrowhead = keyToArrowhead(partial.startArrowheadKey)
    }
    if ((cur.type === "line" || cur.type === "arrow") && partial.endArrowheadKey !== undefined) {
      updates.endArrowhead = keyToArrowhead(partial.endArrowheadKey)
    }

    if (Object.keys(updates).length === 0) return el
    return newElementWith(cur, updates as any)
  })

  const appPatch: Record<string, unknown> = {}
  if (partial.strokeYabaCode !== undefined && partial.strokeYabaCode > 0) {
    const hx = yabaCodeToHex(partial.strokeYabaCode)
    if (hx) appPatch.currentItemStrokeColor = hx
  }
  if (partial.backgroundYabaCode !== undefined) {
    appPatch.currentItemBackgroundColor =
      partial.backgroundYabaCode === 0
        ? "transparent"
        : yabaCodeToHex(partial.backgroundYabaCode) ?? "transparent"
  }
  if (partial.fillStyleKey !== undefined) {
    ;(appPatch as Record<string, unknown>).currentItemFillStyle = partial.fillStyleKey
  }
  if (partial.strokeWidthKey !== undefined) {
    appPatch.currentItemStrokeWidth =
      partial.strokeWidthKey === "thin"
        ? STROKE_WIDTH.thin
        : partial.strokeWidthKey === "bold"
          ? STROKE_WIDTH.bold
          : STROKE_WIDTH.extraBold
  }
  if (partial.strokeStyle !== undefined) appPatch.currentItemStrokeStyle = partial.strokeStyle
  if (partial.roughnessKey !== undefined) {
    appPatch.currentItemRoughness = roughnessFromKey(partial.roughnessKey)
  }
  if (partial.opacityStep !== undefined) {
    appPatch.currentItemOpacity = Math.max(0, Math.min(100, partial.opacityStep * 10))
  }
  if (partial.edgeKey !== undefined) {
    appPatch.currentItemRoundness =
      partial.edgeKey === "round" ? { type: ROUNDNESS.PROPORTIONAL_RADIUS } : null
  }
  if (partial.fontSizeKey !== undefined) {
    appPatch.currentItemFontSize = FONT_SIZE_PRESETS[partial.fontSizeKey]
    appPatch.currentItemFontFamily = FONT_FAMILY.Excalifont
    appPatch.currentItemTextAlign = TEXT_ALIGN.CENTER
    appPatch.currentItemVerticalAlign = VERTICAL_ALIGN.MIDDLE
  }

  api.updateScene({
    elements: next as any,
    appState: appPatch as any,
  })
}

export interface YabaCanvasBridge {
  isReady: () => boolean
  setSceneJson: (sceneJson: string) => void
  getSceneJson: () => string
  setActiveTool: (tool: CanvasTool) => void
  undo: () => void
  redo: () => void
  deleteSelected: () => void
  insertImageFromDataUrl: (dataUrl: string) => void
  applySelectionStyle: (json: string) => void
  canvasLayer: (action: "sendToBack" | "sendBackward" | "bringForward" | "bringToFront") => void
  toggleGridMode: () => void
  toggleObjectsSnapMode: () => void
  applyCanvasInline: (json: string) => void
  getCanvasSelectionLinkContext: () => string
  /** JSON request `{ format: "png" | "svg", exportBackground: boolean }` → JSON result `{ ok, base64?, error? }`. */
  exportImage: (requestJson: string) => Promise<string>
}

export function initCanvasBridge(api: ExcalidrawImperativeAPI): void {
  excalidrawApi = api
  lastMetricsJson = null
  lastStyleJson = null
  clearAutosaveTimer()
  postToYabaNativeHost({ type: "bridgeReady", feature: "canvas" })

  const win = window as Window & { YabaCanvasBridge?: YabaCanvasBridge }
  win.YabaCanvasBridge = {
    isReady: () => !!excalidrawApi,
    setSceneJson: (sceneJson: string) => {
      try {
        const parsed = sceneJson?.trim()
          ? (JSON.parse(sceneJson) as ExcalidrawInitialDataState)
          : DEFAULT_SCENE
        const app = (parsed.appState ?? {}) as Record<string, unknown>
        excalidrawApi?.updateScene({
          elements: (parsed.elements ?? []) as any,
          appState: {
            ...app,
            gridModeEnabled: (app.gridModeEnabled as boolean | undefined) ?? true,
            objectsSnapModeEnabled: (app.objectsSnapModeEnabled as boolean | undefined) ?? true,
          } as any,
          collaborators: new Map(),
        })
        if (parsed.files) {
          excalidrawApi?.addFiles(Object.values(parsed.files))
        }
        publishCanvasHostState()
        if (!shellLoadPublished) {
          shellLoadPublished = true
          postToYabaNativeHost({ type: "shellLoad", result: "loaded" })
        }
      } catch {
        if (!shellLoadPublished) {
          shellLoadPublished = true
          postToYabaNativeHost({ type: "shellLoad", result: "error" })
        }
      }
    },
    getSceneJson: () => {
      if (!excalidrawApi) return JSON.stringify(DEFAULT_SCENE)
      return JSON.stringify({
        type: "excalidraw",
        version: 2,
        source: "https://excalidraw.com",
        elements: excalidrawApi.getSceneElementsIncludingDeleted(),
        appState: excalidrawApi.getAppState(),
        files: excalidrawApi.getFiles(),
      })
    },
    setActiveTool: (tool: CanvasTool) => {
      excalidrawApi?.setActiveTool(toExcalidrawTool(tool))
      publishCanvasHostState()
    },
    undo: () => {
      executeUndoRedo("undo")
      publishCanvasHostState()
    },
    redo: () => {
      executeUndoRedo("redo")
      publishCanvasHostState()
    },
    deleteSelected: () => {
      const api = excalidrawApi
      if (!api) return
      const selected = api.getAppState().selectedElementIds
      const next = api.getSceneElementsIncludingDeleted().map((el) =>
        selected[el.id] ? ({ ...el, isDeleted: true } as any) : el
      )
      api.updateScene({ elements: next as any })
      publishCanvasHostState()
    },
    insertImageFromDataUrl: (dataUrl: string) => {
      if (!dataUrl.startsWith("data:image/")) return
      makeImageElementFromDataUrl(dataUrl)
      publishCanvasHostState()
    },
    applySelectionStyle: (json: string) => {
      try {
        const partial = JSON.parse(json) as ApplySelectionStylePayload
        applySelectionStylePayload(partial)
        publishCanvasHostState()
      } catch {
        /* ignore */
      }
    },
    canvasLayer: (action) => {
      executeNamedAction(action)
      publishCanvasHostState()
    },
    toggleGridMode: () => {
      const api = excalidrawApi
      if (!api) return
      const s = api.getAppState()
      api.updateScene({ appState: { gridModeEnabled: !s.gridModeEnabled } })
      publishCanvasHostState()
    },
    toggleObjectsSnapMode: () => {
      const api = excalidrawApi
      if (!api) return
      const s = api.getAppState()
      api.updateScene({ appState: { objectsSnapModeEnabled: !s.objectsSnapModeEnabled } })
      publishCanvasHostState()
    },
    applyCanvasInline: (json: string) => {
      const api = excalidrawApi
      if (!api) return
      try {
        const payload = JSON.parse(json) as ApplyCanvasInlinePayload
        applyCanvasInlinePayload(api, payload)
        publishCanvasHostState()
      } catch {
        /* ignore */
      }
    },
    getCanvasSelectionLinkContext: () => {
      const api = excalidrawApi
      if (!api) return "{}"
      try {
        return JSON.stringify(getCanvasSelectionLinkContext(api))
      } catch {
        return "{}"
      }
    },
    exportImage: async (requestJson: string) => {
      try {
        const req = JSON.parse(requestJson) as { format: "png" | "svg"; exportBackground: boolean }
        const fmt = req.format === "svg" ? "svg" : "png"
        return await yabaExportCanvasImage(fmt, Boolean(req.exportBackground))
      } catch (e) {
        return JSON.stringify({ ok: false, error: String(e) })
      }
    },
  }
}

export function onCanvasChanged(): void {
  scheduleAutosaveIdle()
  publishCanvasHostState()
}
