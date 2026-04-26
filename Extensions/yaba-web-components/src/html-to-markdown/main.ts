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

function htmlToMarkdown(htmlStr: string): string {
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
    .processSync(htmlStr)
  return String(file)
}

const g = globalThis as typeof globalThis & { HTMLToMarkdown?: HtmlToMarkdownGlobal }
g.HTMLToMarkdown = (html: string) => htmlToMarkdown(html ?? "")

export {}
