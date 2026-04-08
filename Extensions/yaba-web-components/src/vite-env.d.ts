/// <reference types="vite/client" />

declare global {
  interface Window {
    /** Base URL (with trailing slash) for Excalidraw `dist/prod` assets; must be set before the bundle runs. */
    EXCALIDRAW_ASSET_PATH?: string | string[]
  }
}

declare module "*.css" {
  const url: string
  export default url
}

export {}
