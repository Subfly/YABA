import type {
  ExcalidrawImperativeAPI,
  ExcalidrawInitialDataState,
} from "@excalidraw/excalidraw/types"
import { postToYabaNativeHost } from "./yaba-native-host"

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

function publishMetrics(): void {
  if (!excalidrawApi) return
  const appState = excalidrawApi.getAppState()
  const elements = excalidrawApi.getSceneElements()
  const hasSelection = elements.some((el) => appState.selectedElementIds[el.id])
  const payload: HostCanvasMetrics = {
    type: "canvasMetrics",
    activeTool: normalizeCanvasTool(appState.activeTool?.type ?? "selection"),
    hasSelection,
    canUndo: elements.length > 0,
    canRedo: false,
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
      window.dispatchEvent(
        new KeyboardEvent("keydown", { key: "z", ctrlKey: true, metaKey: true })
      )
      publishMetrics()
    },
    redo: () => {
      window.dispatchEvent(
        new KeyboardEvent("keydown", { key: "z", shiftKey: true, ctrlKey: true, metaKey: true })
      )
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
