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
