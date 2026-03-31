import { postToYabaNativeHost } from "./yaba-native-host"

export function publishPdfReaderMetrics(): void {
  const win = window as Window & {
    YabaPdfBridge?: {
      isReady: () => boolean
      getCanCreateAnnotation: () => boolean
      getCurrentPageNumber: () => number
      getPageCount: () => number
    }
  }
  const b = win.YabaPdfBridge
  if (!b?.isReady?.()) return
  postToYabaNativeHost({
    type: "readerMetrics",
    canCreateAnnotation: b.getCanCreateAnnotation(),
    currentPage: b.getCurrentPageNumber(),
    pageCount: Math.max(1, b.getPageCount() || 1),
  })
}

export function publishEpubReaderMetrics(): void {
  const win = window as Window & {
    YabaEpubBridge?: {
      isReady: () => boolean
      getCanCreateAnnotation: () => boolean
      getCurrentPageNumber: () => number
      getPageCount: () => number
    }
  }
  const b = win.YabaEpubBridge
  if (!b?.isReady?.()) return
  postToYabaNativeHost({
    type: "readerMetrics",
    canCreateAnnotation: b.getCanCreateAnnotation(),
    currentPage: b.getCurrentPageNumber(),
    pageCount: Math.max(1, b.getPageCount() || 1),
  })
}
