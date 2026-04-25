import { Readability } from "@mozilla/readability"

const CODE_CLASS_HINT = /(language-|lang-|highlight|hljs|codehilite|prism|rouge|prettyprint|sourcecode|syntax)/i
const INLINE_TEXT_CONTAINERS = new Set(["P", "LI", "TD", "TH", "A", "SPAN", "EM", "STRONG", "B", "I", "U", "S", "DEL"])

const READABILITY_OPTIONS = {
  keepClasses: true,
  charThreshold: 20,
  nbTopCandidates: 10,
}

/**
 * Resolves relative `href` / `src` / `srcset` / `poster` against `baseUrl` in-place.
 * Does not inject `<base href>`.
 */
export function resolveRelativeUrlsInDocument(doc: Document, baseUrl: string): void {
  const base = baseUrl.trim()
  if (!base) return

  const toAbs = (raw: string): string => {
    const u = raw.trim()
    if (
      !u ||
      u.startsWith("#") ||
      u.startsWith("data:") ||
      u.startsWith("mailto:") ||
      u.startsWith("javascript:") ||
      u.startsWith("http://") ||
      u.startsWith("https://")
    ) {
      return raw
    }
    try {
      return new URL(u, base).href
    } catch {
      return raw
    }
  }

  const resolveSrcset = (v: string): string =>
    v
      .split(",")
      .map((part) => {
        const p = part.trim()
        if (!p) return part
        const sp = p.indexOf(" ")
        if (sp === -1) return toAbs(p)
        return `${toAbs(p.slice(0, sp))}${p.slice(sp)}`
      })
      .join(", ")

  doc.querySelectorAll("base").forEach((el) => el.remove())
  doc.querySelectorAll("[href]").forEach((el) => {
    const h = el.getAttribute("href")
    if (h) el.setAttribute("href", toAbs(h))
  })
  doc.querySelectorAll("[src]").forEach((el) => {
    const s = el.getAttribute("src")
    if (s && !s.startsWith("data:")) el.setAttribute("src", toAbs(s))
  })
  doc.querySelectorAll("[srcset]").forEach((el) => {
    const s = el.getAttribute("srcset")
    if (s) el.setAttribute("srcset", resolveSrcset(s))
  })
  doc.querySelectorAll("[poster]").forEach((el) => {
    const p = el.getAttribute("poster")
    if (p) el.setAttribute("poster", toAbs(p))
  })
}

function isLikelyBlockCodeNode(codeNode: Element): boolean {
  const classText = `${codeNode.className || ""} ${codeNode.parentElement?.className || ""}`.trim()
  if (CODE_CLASS_HINT.test(classText)) return true

  if (codeNode.closest("pre")) return true

  const parentTag = codeNode.parentElement?.tagName ?? ""
  if (INLINE_TEXT_CONTAINERS.has(parentTag)) return false

  const codeText = (codeNode.textContent || "").trim()
  if (codeText.length <= 24) return false

  return codeText.includes("\n")
}

function normalizeCodeWrappers(root: ParentNode): void {
  const codeNodes = Array.from(root.querySelectorAll("code"))
  codeNodes.forEach((codeNode) => {
    if (codeNode.closest("pre")) return
    if (!isLikelyBlockCodeNode(codeNode)) return

    const pre = root.ownerDocument?.createElement("pre") ?? document.createElement("pre")
    codeNode.parentNode?.insertBefore(pre, codeNode)
    pre.appendChild(codeNode)
  })
}

interface CodeNodeStats {
  preCount: number
  codeCount: number
  blockCodeCount: number
}

function getCodeNodeStats(html: string): CodeNodeStats {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  normalizeCodeWrappers(doc.body)
  const codeNodes = Array.from(doc.querySelectorAll("code"))
  const blockCodeCount = codeNodes.filter((codeNode) => isLikelyBlockCodeNode(codeNode)).length
  return {
    preCount: doc.querySelectorAll("pre").length,
    codeCount: codeNodes.length,
    blockCodeCount,
  }
}

function readabilityDroppedTooMuchCode(originalHtml: string, readerHtml: string): boolean {
  const original = getCodeNodeStats(originalHtml)
  if (original.preCount <= 0 && original.codeCount <= 0 && original.blockCodeCount <= 0) return false

  const extracted = getCodeNodeStats(readerHtml)
  if (original.blockCodeCount > 0 && extracted.blockCodeCount === 0) return true
  if (original.preCount > 0 && extracted.preCount === 0) return true
  if (
    original.blockCodeCount >= 2 &&
    extracted.blockCodeCount < Math.max(1, Math.floor(original.blockCodeCount * 0.5))
  ) {
    return true
  }
  if (original.codeCount >= 3 && extracted.codeCount === 0) return true
  if (original.codeCount >= 6 && extracted.codeCount < Math.max(2, Math.floor(original.codeCount * 0.35)))
    return true
  return false
}

function extractSemanticMainHtml(html: string): string | null {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  const candidates = Array.from(doc.querySelectorAll("article, main, [role='main']"))
  if (candidates.length <= 0) return null

  const bestCandidate = candidates
    .map((candidate) => {
      const candidateHtml = candidate.innerHTML
      const stats = getCodeNodeStats(candidateHtml)
      const textLength = (candidate.textContent || "").trim().length
      const score = textLength + stats.blockCodeCount * 250 + stats.preCount * 200 + stats.codeCount * 40
      return { candidateHtml, score, textLength, stats }
    })
    .sort((a, b) => b.score - a.score)
    .find(({ textLength, stats }) => textLength >= 280 || stats.blockCodeCount > 0 || stats.preCount > 0)

  return bestCandidate?.candidateHtml ?? null
}

export function extractReadableHtmlFromString(html: string, baseUrl?: string): { readableHtml: string; title: string | null } {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  if (baseUrl?.trim()) {
    resolveRelativeUrlsInDocument(doc, baseUrl.trim())
  }
  normalizeCodeWrappers(doc.body)
  const normalizedOriginalHtml = doc.body.innerHTML

  const docClone = doc.cloneNode(true) as Document
  const reader = new Readability(docClone, READABILITY_OPTIONS)
  const article = reader.parse()
  if (article?.content && article.content.trim().length > 0) {
    const readerDoc = new DOMParser().parseFromString(
      `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${article.content}</body></html>`,
      "text/html"
    )
    normalizeCodeWrappers(readerDoc.body)
    const normalizedReaderHtml = readerDoc.body.innerHTML

    if (readabilityDroppedTooMuchCode(normalizedOriginalHtml, normalizedReaderHtml)) {
      const semanticFallbackHtml = extractSemanticMainHtml(normalizedOriginalHtml)
      if (semanticFallbackHtml) {
        return { readableHtml: semanticFallbackHtml, title: article.title ?? null }
      }
      return { readableHtml: normalizedOriginalHtml, title: article.title ?? null }
    }
    return { readableHtml: normalizedReaderHtml, title: article.title ?? null }
  }
  const semanticFallbackHtml = extractSemanticMainHtml(normalizedOriginalHtml)
  if (semanticFallbackHtml) {
    return { readableHtml: semanticFallbackHtml, title: null }
  }
  return { readableHtml: normalizedOriginalHtml, title: null }
}
