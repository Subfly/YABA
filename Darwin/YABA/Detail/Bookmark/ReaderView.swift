//
//  ReaderView.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//


import SwiftUI
import WebKit

struct ReaderView: View {
    @State
    private var state: ReaderState = .init()
    
    #if targetEnvironment(macCatalyst)
    private let alignment: Alignment = .leading
    #else
    private let alignment: Alignment = .bottom
    #endif
    
    let html: String

    var body: some View {
        Group {
            if state.isLoading {
                ZStack {
                    ProgressView()
                }
            } else if !state.readerAvailable {
                ReaderNotAvailableView()
            } else if let html = state.extractedHTML {
                DisplayWebViewRepresentable(html: state.styledHTML(with: html))
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding()
        .overlay(alignment: alignment) {
            if state.readerAvailable {
                if #available(iOS 26, *) {
                    glassyCustomizationButtonsRow
                } else {
                    legacyCustomizationButtonsRow
                }
            }
        }
        .onAppear {
            state.extractReadableContent(from: html)
        }
        .onChange(of: html) { _, newValue in
            state.extractReadableContent(from: html)
        }
    }
    
    @ViewBuilder
    @available(iOS 26, *)
    private var glassyCustomizationButtonsRow: some View {
        #if targetEnvironment(macCatalyst)
        VStack(spacing: 15) {
            glassyCustomizationButtons
        }
        .padding()
        .glassEffect(.regular.tint(.accentColor.opacity(0.5)), in: .capsule)
        .offset(x: -24)
        #else
        HStack(spacing: 15) {
            glassyCustomizationButtons
        }
        .padding()
        .glassEffect(.regular.tint(.accentColor.opacity(0.5)), in: .capsule)
        #endif
    }
    
    @ViewBuilder
    private var legacyCustomizationButtonsRow: some View {
        #if targetEnvironment(macCatalyst)
        VStack {
            legacyCustomizationButtons
        }
        .frame(width: 28)
        .padding()
        .background {
            Capsule().fill(Color.accentColor)
        }
        .offset(x: -24)
        #else
        HStack {
            legacyCustomizationButtons
        }
        .frame(height: 28)
        .padding()
        .background {
            Capsule().fill(Color.accentColor)
        }
        #endif
    }
    
    @ViewBuilder
    @available(iOS 26, *)
    private var glassyCustomizationButtons: some View {
        GlassEffectContainer(spacing: 15) {
            if UIDevice.current.userInterfaceIdiom == .pad {
                MacOSHoverableToolbarIcon(
                    bundleKey: "text-square",
                    tooltipKey: "Tooltip Title Font Size",
                    onPressed: {
                        state.changeFontSize()
                    }
                ).tint(.white)
                MacOSHoverableToolbarIcon(
                    bundleKey: "cursor-text",
                    tooltipKey: "Tooltip Title Line Height",
                    onPressed: {
                        state.changeLineHeight()
                    }
                ).tint(.white)
                MacOSHoverableToolbarIcon(
                    bundleKey: "colors",
                    tooltipKey: "Tooltip Title Change Background Color",
                    onPressed: {
                        state.changeTheme()
                    }
                ).tint(.white)
            } else {
                Button {
                    state.changeFontSize()
                } label: {
                    YabaIconView(bundleKey: "text-square")
                        .frame(width: 24, height: 24)
                        .foregroundStyle(.white)
                }
                Button {
                    state.changeLineHeight()
                } label: {
                    YabaIconView(bundleKey: "cursor-text")
                        .frame(width: 24, height: 24)
                        .foregroundStyle(.white)
                }
                Button {
                    state.changeTheme()
                } label: {
                    YabaIconView(bundleKey: "colors")
                        .frame(width: 24, height: 24)
                        .foregroundStyle(.white)
                }
            }
        }
    }
    
    @ViewBuilder
    private var legacyCustomizationButtons: some View {
        if UIDevice.current.userInterfaceIdiom == .pad {
            MacOSHoverableToolbarIcon(
                bundleKey: "text-square",
                tooltipKey: "Tooltip Title Font Size",
                onPressed: {
                    state.changeFontSize()
                }
            ).tint(.white)
            Divider()
            MacOSHoverableToolbarIcon(
                bundleKey: "cursor-text",
                tooltipKey: "Tooltip Title Line Height",
                onPressed: {
                    state.changeLineHeight()
                }
            ).tint(.white)
            Divider()
            MacOSHoverableToolbarIcon(
                bundleKey: "colors",
                tooltipKey: "Tooltip Title Change Background Color",
                onPressed: {
                    state.changeTheme()
                }
            ).tint(.white)
        } else {
            Button {
                state.changeFontSize()
            } label: {
                YabaIconView(bundleKey: "text-square")
                    .frame(width: 24, height: 24)
            }.tint(.white)
            Divider()
            Button {
                state.changeLineHeight()
            } label: {
                YabaIconView(bundleKey: "cursor-text")
                    .frame(width: 24, height: 24)
            }.tint(.white)
            Divider()
            Button {
                state.changeTheme()
            } label: {
                YabaIconView(bundleKey: "colors")
                    .frame(width: 24, height: 24)
            }.tint(.white)
        }
    }
}

internal struct ReaderNotAvailableView: View {
    var body: some View {
        ContentUnavailableView {
            Label {
                Text("Reader Not Available Title")
                    .padding(.bottom)
            } icon: {
                YabaIconView(bundleKey: "cancel-square")
                    .scaledToFit()
                    .frame(width: 52, height: 52)
                    .padding(.top)
            }
        } description: {
            Text("Reader Not Available Description")
                .padding(
                    .horizontal,
                    UIDevice.current.userInterfaceIdiom == .pad ? 52 : 0
                )
        }
    }
}

internal struct DisplayWebViewRepresentable: UIViewRepresentable {
    let html: String

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.loadHTMLString(html, baseURL: nil)
    }
}
