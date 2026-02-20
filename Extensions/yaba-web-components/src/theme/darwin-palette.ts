/**
 * Darwin uses CSS system colors + color-scheme.
 * No explicit palette; we rely on WebKit to provide system-appropriate colors.
 */

export const darwinCssVars = {
  light: {
    background: "Canvas",
    onBackground: "CanvasText",
    surface: "Canvas",
    onSurface: "CanvasText",
    surfaceVariant: "ButtonFace",
    onSurfaceVariant: "ButtonText",
    outline: "GrayText",
    primary: "LinkText",
    onPrimary: "ButtonFace",
    primaryContainer: "Highlight",
    onPrimaryContainer: "HighlightText",
  },
  dark: {
    background: "Canvas",
    onBackground: "CanvasText",
    surface: "Canvas",
    onSurface: "CanvasText",
    surfaceVariant: "ButtonFace",
    onSurfaceVariant: "ButtonText",
    outline: "GrayText",
    primary: "LinkText",
    onPrimary: "ButtonFace",
    primaryContainer: "Highlight",
    onPrimaryContainer: "HighlightText",
  },
}
