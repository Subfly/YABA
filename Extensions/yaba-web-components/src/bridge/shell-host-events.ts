/**
 * One-shot initial content load signal for native WebView hosts.
 * Must match Core [YabaWebBridgeScripts.SHELL_LOAD_EVENT_PREFIX].
 */
export const YABA_SHELL_LOAD_EVENT_PREFIX = "yaba-shell-load:"

export interface ShellLoadHostEvent {
  type: "shellLoad"
  result: "loaded" | "error"
}

export function publishShellLoad(result: "loaded" | "error"): void {
  const payload: ShellLoadHostEvent = { type: "shellLoad", result }
  console.info(`${YABA_SHELL_LOAD_EVENT_PREFIX}${JSON.stringify(payload)}`)
}

/**
 * Note editor only: after [NOTE_AUTOSAVE_IDLE_MS] with no editor transactions, native should persist.
 * Must match Core [YabaWebBridgeScripts.NOTE_AUTOSAVE_IDLE_EVENT_PREFIX].
 */
export const YABA_NOTE_AUTOSAVE_IDLE_EVENT_PREFIX = "yaba-note-autosave-idle:"

const NOTE_AUTOSAVE_IDLE_MS = 1000

export interface NoteAutosaveIdleHostEvent {
  type: "noteAutosaveIdle"
}

let noteAutosaveIdleTimer: ReturnType<typeof setTimeout> | null = null

function clearNoteAutosaveIdleTimer(): void {
  if (noteAutosaveIdleTimer !== null) {
    clearTimeout(noteAutosaveIdleTimer)
    noteAutosaveIdleTimer = null
  }
}

/** When true, [scheduleNoteAutosaveAfterEditorActivity] may arm the idle timer (after initial document apply). */
let noteEditorAutosaveIdleEnabled = false

export function setNoteEditorAutosaveIdleEnabled(enabled: boolean): void {
  noteEditorAutosaveIdleEnabled = enabled
  if (!enabled) clearNoteAutosaveIdleTimer()
}

export function scheduleNoteAutosaveAfterEditorActivity(): void {
  const page = typeof document !== "undefined" ? document.body?.dataset.yabaPage : undefined
  if (page !== "editor" || !noteEditorAutosaveIdleEnabled) return
  clearNoteAutosaveIdleTimer()
  noteAutosaveIdleTimer = setTimeout(() => {
    noteAutosaveIdleTimer = null
    const payload: NoteAutosaveIdleHostEvent = { type: "noteAutosaveIdle" }
    console.info(`${YABA_NOTE_AUTOSAVE_IDLE_EVENT_PREFIX}${JSON.stringify(payload)}`)
  }, NOTE_AUTOSAVE_IDLE_MS)
}

