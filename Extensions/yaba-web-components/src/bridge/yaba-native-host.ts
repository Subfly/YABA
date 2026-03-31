/**
 * Native host bridge contract for WebView hosts (Android today; Apple later).
 * Hosts should inject `window.YabaNativeHost.postMessage(json)`.
 */
export type { YabaNativeHostFeature, YabaNativeHostPayload } from "./contracts/native-host"
export { postToYabaNativeHost } from "./transport/host-transport"
