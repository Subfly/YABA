export type Platform = "compose" | "darwin"
export type AppearanceMode = "auto" | "light" | "dark"

export interface ThemeParams {
  platform: Platform
  appearance: AppearanceMode
  cursorColor: string | null
}

export function parseUrlParams(): ThemeParams {
  const params = new URLSearchParams(
    typeof window !== "undefined" ? window.location.search : ""
  )

  const platformRaw = params.get("platform")?.toLowerCase()
  const platform: Platform =
    platformRaw === "compose" || platformRaw === "darwin" ? platformRaw : "compose"

  const appearanceRaw = params.get("appearance")?.toLowerCase()
  const appearance: AppearanceMode =
    appearanceRaw === "light" || appearanceRaw === "dark"
      ? appearanceRaw
      : "auto"

  const cursorColor = params.get("cursor")

  return {
    platform,
    appearance,
    cursorColor: cursorColor || null,
  }
}
