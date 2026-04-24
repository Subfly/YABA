/**
 * pdf.js ships a separate legacy bundle; TypeScript only knows `pdfjs-dist` main entry.
 * Map the legacy ESM path to the same public API types.
 */
declare module "pdfjs-dist/legacy/build/pdf.min.mjs" {
  export * from "pdfjs-dist"
}
