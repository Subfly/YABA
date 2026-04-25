import { extractReadableHtmlFromString } from "./readability-extract"
import { postToYabaNativeHost } from "./yaba-native-host"

export interface ReaderConverterInput {
  html: string
  baseUrl?: string
}

export interface ReaderConverterOutput {
  /** Readability article HTML fragment (or fallback body HTML). */
  readableHtml: string
  title: string | null
}

export interface YabaReaderConverterBridge {
  convertHtmlToReadableHtml(input: ReaderConverterInput): Promise<ReaderConverterOutput>
}

function convert(input: ReaderConverterInput): ReaderConverterOutput {
  return extractReadableHtmlFromString(input.html, input.baseUrl)
}

export function initReaderConverterBridge(): void {
  const win = window as Window & { YabaReaderConverterBridge?: YabaReaderConverterBridge }
  win.YabaReaderConverterBridge = {
    convertHtmlToReadableHtml(input: ReaderConverterInput): Promise<ReaderConverterOutput> {
      return Promise.resolve(convert(input))
    },
  }
  postToYabaNativeHost({ type: "bridgeReady", feature: "reader-converter" })
}
