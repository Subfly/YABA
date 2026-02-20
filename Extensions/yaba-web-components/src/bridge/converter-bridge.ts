import DOMPurify from "dompurify"
import { Readability } from "@mozilla/readability"
import TurndownService from "turndown"
import { gfm } from "turndown-plugin-gfm"

const turndown = new TurndownService({
  headingStyle: "atx",
  codeBlockStyle: "fenced",
  bulletListMarker: "-",
})

turndown.use(gfm)

const ASSET_PLACEHOLDER_PREFIX = "yaba-asset://"

export interface ConverterInput {
  html: string
  baseUrl?: string
}

export interface ConverterAsset {
  placeholder: string
  url: string
  alt?: string
}

export interface ConverterOutput {
  markdown: string
  assets: ConverterAsset[]
}

export interface YabaConverterBridge {
  sanitizeAndConvertHtmlToMarkdown: (input: ConverterInput) => ConverterOutput
}

function resolveUrl(baseUrl: string | undefined, src: string): string {
  if (!baseUrl || src.startsWith("data:") || src.startsWith("http://") || src.startsWith("https://")) {
    return src
  }
  try {
    return new URL(src, baseUrl).href
  } catch {
    return src
  }
}

function toReaderModeHtml(cleanHtml: string, baseUrl?: string): string {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${cleanHtml}</body></html>`,
    "text/html"
  )
  if (baseUrl) {
    const base = doc.createElement("base")
    base.setAttribute("href", baseUrl)
    doc.head.appendChild(base)
  }
  const docClone = doc.cloneNode(true) as Document
  const reader = new Readability(docClone)
  const article = reader.parse()
  if (article?.content && article.content.trim().length > 0) {
    return article.content
  }
  return cleanHtml
}

function isImagePlaceholder(url: string): boolean {
  const u = url.toLowerCase()
  return (
    u.includes("1x1") ||
    u.includes("pixel") ||
    u.includes("spacer") ||
    u.includes("blank") ||
    u.includes("data:image") ||
    u.includes("transparent.gif") ||
    u.includes("tracking")
  )
}

function sanitizeAndConvertWithAssets(html: string, baseUrl?: string): ConverterOutput {
  const clean = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      "h1", "h2", "h3", "h4", "h5", "h6",
      "p", "br", "hr",
      "strong", "b", "em", "i", "u", "s", "strike", "del",
      "code", "pre",
      "ul", "ol", "li",
      "blockquote",
      "a", "img",
      "table", "thead", "tbody", "tr", "th", "td",
      "div", "span", "section", "article", "main",
      "figure", "figcaption", "time",
    ],
    ALLOWED_ATTR: ["href", "src", "alt", "title", "class", "datetime"],
  })

  const readerHtml = toReaderModeHtml(clean, baseUrl)

  const wrapper = document.createElement("div")
  wrapper.innerHTML = readerHtml

  if (baseUrl) {
    wrapper.querySelectorAll("a[href]").forEach((a) => {
      const href = a.getAttribute("href")?.trim()
      if (href && !href.startsWith("#") && !href.startsWith("mailto:") && !href.startsWith("http")) {
        a.setAttribute("href", resolveUrl(baseUrl, href))
      }
    })
  }

  const assets: ConverterAsset[] = []
  const seenUrls = new Map<string, string>()

  const imgs = wrapper.querySelectorAll("img")
  imgs.forEach((img) => {
    const src = img.getAttribute("src")?.trim()
    if (!src || src.startsWith("data:") || isImagePlaceholder(src)) return

    const resolvedUrl = resolveUrl(baseUrl, src)
    const alt = img.getAttribute("alt")?.trim() || undefined

    let placeholder: string
    const existing = seenUrls.get(resolvedUrl)
    if (existing) {
      placeholder = existing
    } else {
      placeholder = `${ASSET_PLACEHOLDER_PREFIX}${assets.length}`
      seenUrls.set(resolvedUrl, placeholder)
      assets.push({ placeholder, url: resolvedUrl, alt })
    }
    img.setAttribute("src", placeholder)
  })

  const markdown = turndown.turndown(wrapper)
  return { markdown, assets }
}

export function initConverterBridge(): void {
  const win = window as Window & { YabaConverterBridge?: YabaConverterBridge }
  win.YabaConverterBridge = {
    sanitizeAndConvertHtmlToMarkdown(input: ConverterInput): ConverterOutput {
      return sanitizeAndConvertWithAssets(input.html, input.baseUrl)
    },
  }
}
