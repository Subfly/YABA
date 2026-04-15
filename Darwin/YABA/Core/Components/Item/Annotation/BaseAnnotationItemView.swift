//
//  BaseAnnotationItemView.swift
//  YABA
//
//  Shared annotation row: leading color bar + text (Compose `BaseAnnotationItemView` parity).
//

import SwiftUI

struct BaseAnnotationItemView: View {
    private let quotePreview: String
    private let note: String?
    private let accentColor: YabaColor

    let interactive: Bool
    let onTap: () -> Void

    init(annotation: AnnotationModel, interactive: Bool, onTap: @escaping () -> Void) {
        self.interactive = interactive
        self.onTap = onTap

        let trimmedNote = annotation.note?.trimmingCharacters(in: .whitespacesAndNewlines)
        let rawBase = annotation.quoteText ?? annotation.note
        let raw = rawBase?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if raw.count > 120 {
            quotePreview = String(raw.prefix(120)) + "…"
        } else {
            quotePreview = raw
        }
        note = trimmedNote
        accentColor = annotation.colorRole
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(accentColor.getUIColor())
                .frame(width: 4)
                .frame(minHeight: 48)
            VStack(alignment: .leading, spacing: 4) {
                if !quotePreview.isEmpty {
                    Text(quotePreview)
                        .font(.body)
                        .lineLimit(3)
                    if let note, !note.isEmpty {
                        Text(note)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                } else {
                    Text("TODO")
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .contentShape(Rectangle())
        .onTapGesture {
            if interactive {
                onTap()
            }
        }
    }
}
