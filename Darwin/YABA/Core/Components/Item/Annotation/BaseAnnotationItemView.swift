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
    
    @State
    private var textBlockHeight: CGFloat = 0

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
                .frame(height: max(18, textBlockHeight))
            VStack(alignment: .leading, spacing: 4) {
                if !quotePreview.isEmpty {
                    Text(quotePreview)
                        .font(.body.weight(.medium))
                        .lineLimit(2)
                    if let note, !note.isEmpty {
                        Text(note)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(3)
                    }
                } else {
                    Text("Annotation Quote Empty Message")
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }
            .background {
                GeometryReader { proxy in
                    Color.clear
                        .onAppear {
                            textBlockHeight = proxy.size.height
                        }
                        .onChange(of: proxy.size.height) { _, newHeight in
                            textBlockHeight = newHeight
                        }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture {
            if interactive {
                onTap()
            }
        }
    }
}
