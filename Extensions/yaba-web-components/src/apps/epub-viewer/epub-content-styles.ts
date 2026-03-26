/** Resolve bundled Quicksand files next to epub-viewer.html (e.g. dist/fonts/). */
function fontUrl(file: string): string {
  return new URL(`fonts/${file}`, window.location.href).href
}

/**
 * @font-face for EPUB iframe documents (blob origin cannot use the parent bundle’s relative URLs).
 * Falls back to system fonts if files are missing at runtime.
 */
export function getEpubFontFaceCss(): string {
  return `
@font-face {
  font-family: "Quicksand";
  src: url("${fontUrl("Quicksand-Light.ttf")}") format("truetype");
  font-weight: 300;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${fontUrl("Quicksand-Regular.ttf")}") format("truetype");
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${fontUrl("Quicksand-Medium.ttf")}") format("truetype");
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${fontUrl("Quicksand-SemiBold.ttf")}") format("truetype");
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}
@font-face {
  font-family: "Quicksand";
  src: url("${fontUrl("Quicksand-Bold.ttf")}") format("truetype");
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}
`
}

/**
 * Reader typography + layout overrides inside EPUB spine iframes.
 * [data-yaba-reader-theme] on the iframe root mirrors host prefs.
 */
export function getEpubContentOverrideCss(): string {
  return `
${getEpubFontFaceCss()}

html {
  -webkit-text-size-adjust: 100%;
  overflow-wrap: anywhere;
  word-wrap: break-word;
  -webkit-user-select: text;
  user-select: text;
}

html, body {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
  font-size: var(--yaba-reader-font-size, 18px) !important;
  line-height: var(--yaba-reader-line-height, 1.6) !important;
  color: var(--yaba-reader-on-bg) !important;
  background: var(--yaba-reader-bg, transparent) !important;
  box-sizing: border-box !important;
  max-width: 100% !important;
  -webkit-user-select: text;
  user-select: text;
}

*, *::before, *::after {
  box-sizing: border-box !important;
}

/* Body text: Quicksand; monospace stacks for code only (matches TipTap reader). */
body, p, li, td, th, blockquote, figcaption, h1, h2, h3, h4, h5, h6, span, div, em, strong, small, label, dt, dd {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
}

a {
  font-family: var(--yaba-font-family, "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif) !important;
  color: var(--yaba-primary, #485d92) !important;
}

code, kbd, samp, tt,
pre code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important;
}

pre, code, kbd, samp {
  max-width: 100% !important;
}

pre {
  white-space: pre !important;
  overflow-x: auto !important;
  overflow-y: hidden !important;
  width: 100% !important;
  max-width: 100% !important;
  box-sizing: border-box !important;
  page-break-inside: avoid;
}

/* Prevent wide code/tables from painting over adjacent columns in paginated flow */
table {
  display: block !important;
  width: 100% !important;
  max-width: 100% !important;
  overflow-x: auto !important;
  border-collapse: collapse;
}

img, svg, video {
  max-width: 100% !important;
  height: auto !important;
}
`
}
