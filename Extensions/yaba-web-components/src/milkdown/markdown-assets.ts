/** Canonical prefix for persisted inline assets in markdown image URLs. */
export const ASSET_MARKDOWN_PREFIX = "../assets/" as const

export function rewriteAssetPathsInMarkdown(md: string, assetsBaseUrl: string): string {
  if (!md.includes(ASSET_MARKDOWN_PREFIX)) return md
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return md.replaceAll(ASSET_MARKDOWN_PREFIX, `${base}assets/`)
}

export function rewriteAssetPathsInReaderHtml(html: string, assetsBaseUrl: string): string {
  if (!html.includes(ASSET_MARKDOWN_PREFIX)) return html
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return html.replaceAll(ASSET_MARKDOWN_PREFIX, `${base}assets/`)
}

/** Persist canonical `../assets/…` paths for storage / native pruning. */
export function normalizeMarkdownAssetPathsForPersistence(md: string, lastAssetsBaseUrl: string | undefined): string {
  if (!lastAssetsBaseUrl || !md.includes("assets/")) return md
  const base = lastAssetsBaseUrl.replace(/\/?$/, "/")
  const absolutePrefix = `${base}assets/`
  if (!md.includes(absolutePrefix)) return md
  return md.replaceAll(absolutePrefix, ASSET_MARKDOWN_PREFIX)
}

export function resolveImageSrcForEditor(src: string, lastAssetsBaseUrl: string | undefined): string {
  if (!lastAssetsBaseUrl || !src.includes(ASSET_MARKDOWN_PREFIX)) return src
  const base = lastAssetsBaseUrl.replace(/\/?$/, "/")
  return src.replaceAll(ASSET_MARKDOWN_PREFIX, `${base}assets/`)
}

export function normalizeImageUrlForPersistence(src: string, lastAssetsBaseUrl: string | undefined): string | null {
  const s = src.trim()
  if (!s) return null
  if (s.startsWith(ASSET_MARKDOWN_PREFIX)) {
    return s
  }
  if (lastAssetsBaseUrl) {
    const base = lastAssetsBaseUrl.replace(/\/?$/, "/")
    const absolutePrefix = `${base}assets/`
    if (s.startsWith(absolutePrefix)) {
      return `${ASSET_MARKDOWN_PREFIX}${s.slice(absolutePrefix.length)}`
    }
  }
  const idx = s.indexOf("/assets/")
  if (idx >= 0) {
    const after = s.slice(idx + "/assets/".length).split("?")[0].split("#")[0]
    if (after && !after.includes("/")) {
      return `${ASSET_MARKDOWN_PREFIX}${after}`
    }
  }
  return null
}

/** Collect canonical `../assets/file` referenced in markdown images. */
export function collectUsedInlineAssetSrcsFromMarkdown(
  md: string,
  lastAssetsBaseUrl: string | undefined,
): string[] {
  const out = new Set<string>()
  const imageRe = /!\[[^\]]*]\(([^)]+)\)/g
  let m: RegExpExecArray | null
  while ((m = imageRe.exec(md)) !== null) {
    const raw = m[1]?.trim().replace(/^<|>$/g, "") ?? ""
    const c = normalizeImageUrlForPersistence(raw, lastAssetsBaseUrl)
    if (c) out.add(c)
  }
  return [...out].sort()
}
