/**
 * Shared Showdown configuration for GFM-oriented HTML ↔ Markdown at integration boundaries.
 * Converter and editor paste/reader HTML paths use the same options for consistent output.
 */
import showdown from "showdown"

const GFM_OPTIONS: showdown.ConverterOptions = {
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
}

export function createShowdownGfmConverter(): showdown.Converter {
  return new showdown.Converter(GFM_OPTIONS)
}
