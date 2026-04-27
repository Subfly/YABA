//
//  MarkdownEditor.swift
//  YABACore
//
//  Standalone `UITextView` (TextKit 2) Markdown editor with syntax styling.
//

import SwiftUI
import UIKit

public struct MarkdownEditor: View {
    @Binding public var text: String
    public var configuration: MarkdownEditorConfiguration

    public init(
        text: Binding<String>,
        configuration: MarkdownEditorConfiguration = .init()
    ) {
        self._text = text
        self.configuration = configuration
    }

    public var body: some View {
        MarkdownEditorRepresentable(
            text: $text,
            configuration: configuration
        )
    }
}

struct MarkdownEditorRepresentable: UIViewRepresentable {
    @Binding var text: String
    var configuration: MarkdownEditorConfiguration

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> UITextView {
        let v = UITextView()
        v.backgroundColor = UIColor.secondarySystemBackground
        v.font = configuration.font
        v.textContainerInset = UIEdgeInsets(top: 12, left: 10, bottom: 12, right: 10)
        v.textContainer.lineFragmentPadding = 0
        v.autocorrectionType = .no
        v.autocapitalizationType = .sentences
        v.delegate = context.coordinator
        v.text = text
        context.coordinator.applyHighlight(to: v)
        return v
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        if uiView.text != text {
            let r = uiView.selectedRange
            uiView.text = text
            if r.location <= (uiView.text as NSString).length {
                uiView.selectedRange = r
            }
        }
        context.coordinator.parent = self
        uiView.font = configuration.font
        context.coordinator.applyHighlight(to: uiView)
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: MarkdownEditorRepresentable
        private var lastLength: Int = 0

        init(_ parent: MarkdownEditorRepresentable) {
            self.parent = parent
        }

        func textViewDidChange(_ textView: UITextView) {
            parent.text = textView.text ?? ""
            applyHighlight(to: textView)
        }

        func applyHighlight(to textView: UITextView) {
            let raw = textView.text ?? ""
            let sel = textView.selectedRange
            let attr = MarkdownSyntaxHighlighter.attributedMarkdown(
                text: raw,
                baseFont: parent.configuration.font,
                showInvisibles: parent.configuration.showInvisibleCharacters
            )
            textView.attributedText = attr
            if sel.location <= (textView.text as NSString).length {
                textView.selectedRange = sel
            }
        }
    }
}
