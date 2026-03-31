import type { YabaNativeHostPayload } from "../contracts/native-host"

declare global {
  interface Window {
    YabaNativeHost?: { postMessage: (json: string) => void }
    YabaAndroidHost?: { postMessage: (json: string) => void }
  }
}

/** Post a typed host payload to the injected native transport. */
export function postToYabaNativeHost(payload: YabaNativeHostPayload): void {
  const host =
    typeof window !== "undefined"
      ? (window.YabaNativeHost ?? window.YabaAndroidHost)
      : undefined
  if (!host?.postMessage) return
  try {
    host.postMessage(JSON.stringify(payload))
  } catch {
    /* ignore */
  }
}
