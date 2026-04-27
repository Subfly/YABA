//
//  MarkdownComponentsDemoView.swift
//  YABAMobile
//
//  Visual / iteration host for native Markdown components (Debug).
//

#if DEBUG
import SwiftUI

enum MarkdownComponentsDemoMode: String, CaseIterable, Identifiable {
    case preview = "Preview"
    case editor = "Editor"
    var id: String { rawValue }
}

struct MarkdownComponentsDemoView: View {
    @State private var markdown: String = MarkdownSampleContent.galleryDocument
    @State private var mode: MarkdownComponentsDemoMode = .preview

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Picker("Mode", selection: $mode) {
                ForEach(MarkdownComponentsDemoMode.allCases) { m in
                    Text(m.rawValue).tag(m)
                }
            }
            .pickerStyle(.segmented)
            .padding()

            Group {
                switch mode {
                case .preview:
                    MarkdownPreview(
                        markdown: markdown,
                        configuration: MarkdownPreviewConfiguration(
                            showFrontMatter: true,
                            showLinkReferenceBlocks: true,
                            useWebViewForHtmlBlocks: true
                        )
                    )
                case .editor:
                    MarkdownEditor(text: $markdown)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .navigationTitle("Markdown")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        MarkdownComponentsDemoView()
    }
}
#endif
