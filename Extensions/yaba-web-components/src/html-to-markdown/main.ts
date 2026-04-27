import { Readability } from "@mozilla/readability"
import { parseHTML } from "linkedom"
import rehypeFormat from "rehype-format"
import rehypeIgnore from "rehype-ignore"
import rehypeParse from "rehype-parse"
import rehypePrism from "rehype-prism-plus"
import rehypeRaw from "rehype-raw"
import rehypeRemark from "rehype-remark"
import rehypeSlug from "rehype-slug"
import rehypeVideo from "rehype-video"
import remarkGfm from "remark-gfm"
import remarkStringify from "remark-stringify"
import { unified } from "unified"

type HtmlToMarkdownGlobal = (html: string) => string
type ParsedArticle = ReturnType<Readability["parse"]>

function escapeHtml(input: string): string {
  return input
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;")
}

function formatPublishedTime(publishedTime: string | null | undefined): string | null {
  const raw = (publishedTime ?? "").trim()
  if (!raw) return null
  const parsed = new Date(raw)
  if (Number.isNaN(parsed.getTime())) return raw
  return parsed.toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  })
}

function estimateReadTimeRange(textContent: string, length: number): string | null {
  const text = textContent.trim()
  const words = text ? text.split(/\s+/u).length : Math.round(length / 5)
  if (words <= 0) return null

  const low = Math.max(1, Math.ceil(words / 240))
  const high = Math.max(1, Math.ceil(words / 180))
  if (low === high) return `${low} min read`
  return `${low}-${high} min read`
}

function pickCanonicalUrl(document: Document): string | null {
  const ogUrl = document.querySelector('meta[property="og:url"]')?.getAttribute("content")?.trim() ?? ""
  if (ogUrl) return ogUrl
  const canonical = document.querySelector('link[rel="canonical"]')?.getAttribute("href")?.trim() ?? ""
  if (canonical) return canonical
  return null
}

function extractHostLabel(urlString: string | null, siteName: string | null): string | null {
  if (siteName && siteName.trim()) return siteName.trim()
  if (!urlString) return null
  try {
    const host = new URL(urlString).hostname.trim()
    if (!host) return null
    return host.replace(/^www\./u, "")
  } catch {
    return null
  }
}

function renderReaderHeader(article: ParsedArticle, canonicalUrl: string | null): string {
  if (!article) return ""

  const title = (article.title ?? "").trim()
  const byline = (article.byline ?? "").trim()
  const excerpt = (article.excerpt ?? "").trim()
  const siteName = (article.siteName ?? "").trim() || null
  const hostLabel = extractHostLabel(canonicalUrl, siteName)
  const published = formatPublishedTime(article.publishedTime)
  const readTime = estimateReadTimeRange(article.textContent ?? "", article.length ?? 0)

  const headerParts: string[] = []
  if (hostLabel) {
    if (canonicalUrl) {
      headerParts.push(
        `<p><a href="${escapeHtml(canonicalUrl)}">${escapeHtml(hostLabel)}</a></p>`,
      )
    } else {
      headerParts.push(`<p>${escapeHtml(hostLabel)}</p>`)
    }
  }
  if (title) headerParts.push(`<h1>${escapeHtml(title)}</h1>`)
  if (byline) headerParts.push(`<p><strong>By:</strong> ${escapeHtml(byline)}</p>`)
  if (published || readTime) {
    const meta = [published, readTime].filter((v): v is string => Boolean(v)).join(" · ")
    headerParts.push(`<p>${escapeHtml(meta)}</p>`)
  }
  if (excerpt) headerParts.push(`<blockquote><p>${escapeHtml(excerpt)}</p></blockquote>`)

  if (headerParts.length === 0) return ""
  return `<header>${headerParts.join("")}</header><hr />`
}

/** Readability article HTML with a metadata header, or original HTML if extraction fails. */
function extractReadableHtml(htmlStr: string): string {
  const s = htmlStr ?? ""
  if (!s.trim()) return s
  try {
    const { document } = parseHTML(s)
    const canonicalUrl = pickCanonicalUrl(document)
    const readerDoc = document.cloneNode(true) as Document
    const reader = new Readability(readerDoc)
    const article = reader.parse()
    const content = article?.content?.trim()
    if (content && content.length > 0) {
      return `${renderReaderHeader(article, canonicalUrl)}${content}`
    }
  } catch {
    // fall through
  }
  return s
}

function htmlToMarkdown(htmlStr: string): string {
  const readable = extractReadableHtml(htmlStr)
  const file = unified()
    .use(rehypeParse, { fragment: true })
    .use(rehypeSlug)
    .use(rehypeIgnore)
    .use(rehypeVideo)
    .use(rehypeFormat)
    .use(rehypeRaw)
    .use(rehypePrism)
    .use(rehypeRemark)
    .use(remarkGfm)
    .use(remarkStringify, { rule: "*" })
    .processSync(readable)
  return String(file)
}

const g = globalThis as typeof globalThis & { HTMLToMarkdown?: HtmlToMarkdownGlobal }
g.HTMLToMarkdown = (html: string) => htmlToMarkdown(html ?? "")

export {}
