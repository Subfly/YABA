import type {
  ExcalidrawImperativeAPI,
  ExcalidrawInitialDataState,
} from "@excalidraw/excalidraw/types"
import { postToYabaNativeHost } from "./yaba-native-host"

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

  // With handleKeyboardGlobally=false, Excalidraw listens on the root div, not document.
  // Order: focused root first, then document/window (global listener when enabled).
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
}

const CANVAS_AUTOSAVE_IDLE_MS = 1000

let excalidrawApi: ExcalidrawImperativeAPI | null = null
let autosaveTimer: ReturnType<typeof setTimeout> | null = null
let lastMetricsJson: string | null = null
let shellLoadPublished = false

const DEFAULT_SCENE: ExcalidrawInitialDataState = {
  elements: [],
  appState: {
    viewBackgroundColor: "#ffffff",
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
  }
  const json = JSON.stringify(payload)
  if (json === lastMetricsJson) return
  lastMetricsJson = json
  postToYabaNativeHost(payload)
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

export interface YabaCanvasBridge {
  isReady: () => boolean
  setSceneJson: (sceneJson: string) => void
  getSceneJson: () => string
  setActiveTool: (tool: CanvasTool) => void
  undo: () => void
  redo: () => void
  deleteSelected: () => void
  insertImageFromDataUrl: (dataUrl: string) => void
}

export function initCanvasBridge(api: ExcalidrawImperativeAPI): void {
  excalidrawApi = api
  lastMetricsJson = null
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
        excalidrawApi?.updateScene({
          elements: (parsed.elements ?? []) as any,
          appState: (parsed.appState ?? {}) as any,
          collaborators: new Map(),
        })
        if (parsed.files) {
          excalidrawApi?.addFiles(Object.values(parsed.files))
        }
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
      publishMetrics()
    },
    undo: () => {
      executeUndoRedo("undo")
      publishMetrics()
    },
    redo: () => {
      executeUndoRedo("redo")
      publishMetrics()
    },
    deleteSelected: () => {
      const api = excalidrawApi
      if (!api) return
      const selected = api.getAppState().selectedElementIds
      const next = api.getSceneElementsIncludingDeleted().map((el) =>
        selected[el.id] ? ({ ...el, isDeleted: true } as any) : el
      )
      api.updateScene({ elements: next as any })
      publishMetrics()
    },
    insertImageFromDataUrl: (dataUrl: string) => {
      if (!dataUrl.startsWith("data:image/")) return
      makeImageElementFromDataUrl(dataUrl)
      publishMetrics()
    },
  }
}

export function onCanvasChanged(): void {
  scheduleAutosaveIdle()
  publishMetrics()
}
