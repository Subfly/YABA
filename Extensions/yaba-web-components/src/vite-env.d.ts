/// <reference types="vite/client" />

declare module "*.css" {
  const url: string
  export default url
}

declare module "turndown" {
  interface TurndownRule {
    filter: string | string[] | ((node: HTMLElement) => boolean)
    replacement: (content: string, node: HTMLElement, options: unknown) => string
  }

  interface TurndownOptions {
    headingStyle?: "setext" | "atx"
    codeBlockStyle?: "indented" | "fenced"
    bulletListMarker?: "-" | "+" | "*"
  }

  class TurndownService {
    constructor(options?: TurndownOptions)
    use(plugin: (service: TurndownService) => void): this
    addRule(key: string, rule: TurndownRule): this
    turndown(html: string | HTMLElement): string
  }

  export default TurndownService
}

declare module "turndown-plugin-gfm" {
  import type TurndownService from "turndown"
  export function gfm(service: TurndownService): void
}
