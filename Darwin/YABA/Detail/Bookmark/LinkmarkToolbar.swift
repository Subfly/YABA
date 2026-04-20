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
        HStack(spacing: 10) {
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
                toolbarGlyph("colors")
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
                toolbarGlyph("text-square")
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
                toolbarGlyph("cursor-text")
            }
            if canAnnotate {
                Button(action: onStickyNote) {
                    toolbarGlyph("sticky-note-03")
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(floatingToolbarBackground)
        .shadow(radius: 6)
        .opacity(isVisible ? 1 : 0)
        .offset(y: isVisible ? 0 : 24)
        .animation(.smooth, value: isVisible)
    }

    @ViewBuilder
    private var floatingToolbarBackground: some View {
        if #available(iOS 26, *) {
            Capsule()
                .fill(Color.clear)
                .glassEffect(.regular.tint(folderAccent.opacity(0.35)).interactive(), in: Capsule())
        } else {
            Capsule()
                .fill(.ultraThinMaterial)
        }
    }

    private func toolbarGlyph(_ icon: String) -> some View {
        ZStack {
            Circle()
                .fill(folderAccent.opacity(0.55))
            YabaIconView(bundleKey: icon)
                .foregroundStyle(.white)
                .frame(width: 22, height: 22)
        }
        .frame(width: 44, height: 44)
    }
}
