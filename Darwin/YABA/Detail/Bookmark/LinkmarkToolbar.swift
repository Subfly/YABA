//
//  Created by Ali Taha on 20.04.2026.
//

import SwiftUI

// MARK: - Reader floating toolbar

struct LinkmarkReaderFloatingToolbar: View {
    let folderAccent: Color
    let isVisible: Bool
    let canAnnotate: Bool
    let readerTheme: ReaderTheme
    let readerFontSize: ReaderFontSize
    let readerLineHeight: ReaderLineHeight
    let onSelectTheme: (ReaderTheme) -> Void
    let onSelectFontSize: (ReaderFontSize) -> Void
    let onSelectLineHeight: (ReaderLineHeight) -> Void
    let onStickyNote: () -> Void

    var body: some View {
        Group {
            if #available(iOS 26, *) {
                GlassEffectContainer(spacing: 30) {
                    toolbarMenus(padLabels: true)
                }
                .glassEffect(.regular.tint(folderAccent.opacity(0.2)).interactive())
                .animation(.smooth, value: canAnnotate)
            } else {
                toolbarMenus(padLabels: false)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background {
                        Capsule()
                            .fill(.ultraThinMaterial)
                    }
            }
        }
        .shadow(radius: 6)
        .opacity(isVisible ? 1 : 0)
        .offset(y: isVisible ? 0 : 24)
        .animation(.smooth, value: isVisible)
    }

    @ViewBuilder
    private func toolbarMenus(padLabels: Bool) -> some View {
        HStack(spacing: padLabels ? 0 : 10) {
            Menu {
                ForEach(ReaderTheme.allCases, id: \.self) { t in
                    Button {
                        onSelectTheme(t)
                    } label: {
                        HStack {
                            if readerTheme == t {
                                Image(systemName: "checkmark")
                            }
                            Text(t.getUITitle())
                        }
                    }
                }
            } label: {
                menuLabelIcon("colors", padLabels: padLabels)
            }
            Menu {
                ForEach(ReaderFontSize.allCases, id: \.self) { f in
                    Button {
                        onSelectFontSize(f)
                    } label: {
                        HStack {
                            if readerFontSize == f {
                                Image(systemName: "checkmark")
                            }
                            Text(f.getUITitle())
                        }
                    }
                }
            } label: {
                menuLabelIcon("text-square", padLabels: padLabels)
            }
            Menu {
                ForEach(ReaderLineHeight.allCases, id: \.self) { lh in
                    Button {
                        onSelectLineHeight(lh)
                    } label: {
                        HStack {
                            if readerLineHeight == lh {
                                Image(systemName: "checkmark")
                            }
                            Text(lh.getUITitle())
                        }
                    }
                }
            } label: {
                menuLabelIcon("cursor-text", padLabels: padLabels)
            }
            if canAnnotate {
                Button(action: onStickyNote) {
                    menuLabelIcon("sticky-note-03", padLabels: padLabels)
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private func menuLabelIcon(_ icon: String, padLabels: Bool) -> some View {
        if padLabels {
            toolbarGlyph(icon)
                .padding()
        } else {
            toolbarGlyph(icon)
        }
    }

    private func toolbarGlyph(_ icon: String) -> some View {
        YabaIconView(bundleKey: icon)
            .foregroundStyle(.white)
            .frame(width: 22, height: 22)
    }
}
