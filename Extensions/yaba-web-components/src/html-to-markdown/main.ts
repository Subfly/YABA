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

/** Article HTML from Mozilla Readability, or original HTML if extraction fails. */
function extractReadableHtml(htmlStr: string): string {
  const s = htmlStr ?? ""
  if (!s.trim()) return s
  try {
    const { document } = parseHTML(s)
    const reader = new Readability(document)
    const article = reader.parse()
    const content = article?.content?.trim()
    if (content && content.length > 0) {
      return content
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
