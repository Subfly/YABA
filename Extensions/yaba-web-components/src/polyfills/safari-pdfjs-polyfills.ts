/**
 * Safari / WKWebView: PDF.js expects modern web APIs that may be missing.
 * This module is side-effect only; import it before any `pdfjs-dist` or worker code.
 */
const PromiseWithPoly = Promise as typeof Promise & {
  withResolvers?: <T>() => {
    promise: Promise<T>
    resolve: (value: T | PromiseLike<T>) => void
    reject: (reason?: unknown) => void
  }
}

if (typeof PromiseWithPoly.withResolvers !== "function") {
  PromiseWithPoly.withResolvers = function withResolvers<T>() {
    let resolve!: (value: T | PromiseLike<T>) => void
    let reject!: (reason?: unknown) => void
    const promise = new Promise<T>((res, rej) => {
      resolve = res
      reject = rej
    })
    return { promise, resolve, reject }
  }
}

const SignalCtor = globalThis.AbortSignal as typeof AbortSignal & {
  any?: (signals: Iterable<AbortSignal>) => AbortSignal
}

if (typeof SignalCtor.any !== "function") {
  SignalCtor.any = function any(signals: Iterable<AbortSignal>): AbortSignal {
    const controller = new globalThis.AbortController()
    for (const signal of signals) {
      if (signal.aborted) {
        controller.abort(signal.reason)
        break
      }
      signal.addEventListener("abort", () => controller.abort(signal.reason), { once: true })
    }
    return controller.signal
  }
}

const URLWithPoly = globalThis.URL as typeof URL & {
  parse?: (url: string, base?: string | URL) => globalThis.URL | null
}

if (typeof URLWithPoly.parse !== "function") {
  URLWithPoly.parse = function parse(url: string, baseUrl?: string | URL) {
    try {
      return new globalThis.URL(url, baseUrl)
    } catch {
      return null
    }
  }
}
