/**
 * Compose Material tokens from YABA Compose theme.
 * Source: Compose/YABA/composeApp/src/commonMain/kotlin/.../theme/Color.kt
 */

export interface ComposePalette {
  background: string
  onBackground: string
  surface: string
  onSurface: string
  surfaceVariant: string
  onSurfaceVariant: string
  outline: string
  primary: string
  onPrimary: string
  primaryContainer: string
  onPrimaryContainer: string
}

export const composeLight: ComposePalette = {
  background: "#FAF8FF",
  onBackground: "#1A1B21",
  surface: "#FAF8FF",
  onSurface: "#1A1B21",
  surfaceVariant: "#E1E2EC",
  onSurfaceVariant: "#44464F",
  outline: "#757780",
  primary: "#485D92",
  onPrimary: "#FFFFFF",
  primaryContainer: "#DAE2FF",
  onPrimaryContainer: "#2F4578",
}

export const composeDark: ComposePalette = {
  background: "#121318",
  onBackground: "#E2E2E9",
  surface: "#121318",
  onSurface: "#E2E2E9",
  surfaceVariant: "#44464F",
  onSurfaceVariant: "#C5C6D0",
  outline: "#8F9099",
  primary: "#B1C5FF",
  onPrimary: "#162E60",
  primaryContainer: "#2F4578",
  onPrimaryContainer: "#DAE2FF",
}
