/** Mutable placeholder for the note editor (updated from native via bridge). */
let noteEditorPlaceholderText = ""

export function setNoteEditorPlaceholderText(text: string): void {
  noteEditorPlaceholderText = text
}

export function getNoteEditorPlaceholderText(): string {
  return noteEditorPlaceholderText
}
