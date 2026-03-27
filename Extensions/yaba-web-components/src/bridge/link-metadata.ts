import { clean } from "@protontech/tidy-url"
import {
  audio,
  createScraper,
  date,
  favicons,
  jsonLd,
  logo,
  metaTags,
  openGraph,
  twitter,
  video,
} from "web-meta-scraper"
import type { ResolvedMetadata } from "web-meta-scraper"

export interface LinkMetadata {
  cleanedUrl: string
  title: string | null
  description: string | null
  author: string | null
  date: string | null
  audio: string | null
  video: string | null
  image: string | null
  logo: string | null
}

function toSingle(value: unknown): string | null {
  if (value == null) return null
  if (Array.isArray(value)) {
    const first = value.map((v) => String(v).trim()).find((s) => s.length > 0)
    return first ?? null
  }
  const s = String(value).trim()
  return s.length > 0 ? s : null
}

/** Clean URL for display and as base URL for resolving relative metadata. */
export function tidyUrlString(input: string): string {
  try {
    const data = clean(input.trim())
    return data.url?.trim() || input.trim()
  } catch {
    return input.trim()
  }
}

/**
 * HTML-only scrape (Core already fetched the document). Plugins match prior fields: OG/Twitter/meta,
 * JSON-LD, dates, logo, media, favicons. No `oembed` plugin — it can trigger extra HTTP from WebView.
 */
const linkScraper = createScraper({
  plugins: [metaTags, openGraph, twitter, jsonLd, date, logo, audio, video, favicons],
  postProcess: {
    omitEmpty: true,
    fallbacks: true,
    secureImages: true,
    maxDescriptionLength: 10000,
  },
})

function pickLogo(m: ResolvedMetadata): string | null {
  const fromOg = m.logo?.trim()
  if (fromOg) return fromOg
  const icon = m.favicon?.trim()
  if (icon) return icon
  const first = m.favicons?.[0]?.url?.trim()
  return first && first.length > 0 ? first : null
}

export async function extractLinkMetadata(html: string, pageUrl: string): Promise<LinkMetadata> {
  const cleanedUrl = tidyUrlString(pageUrl)
  let metadata: ResolvedMetadata = {}
  try {
    const { metadata: m } = await linkScraper.scrape(html, { url: cleanedUrl })
    metadata = m
  } catch {
    metadata = {}
  }

  const audioUrl = metadata.audio?.[0]?.url ?? null
  const videoUrl = metadata.videos?.[0]?.url ?? null

  return {
    cleanedUrl,
    title: toSingle(metadata.title),
    description: toSingle(metadata.description),
    author: toSingle(metadata.author),
    date: toSingle(metadata.date),
    audio: toSingle(audioUrl),
    video: toSingle(videoUrl),
    image: toSingle(metadata.image),
    logo: toSingle(pickLogo(metadata)),
  }
}
