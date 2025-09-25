//
//  SimpleBookmarkCreationView.swift
//  YABAShareMac
//
//  Created by Ali Taha on 18.05.2025.
//

import SwiftUI
import SwiftData

@MainActor
@Observable
private class HoverState {
    var isHoveredOnCancel: Bool = false
    var isHoveredOnOpenApp: Bool = false
    var isHoveredOnSave: Bool = false
}

struct SimpleBookmarkCreationMacOSView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @AppStorage(Constants.saveToArchiveOrgKey)
    private var saveToArchiveOrg: Bool = false
    
    @State
    private var creationState: BookmarkCreationState = .init(isInEditMode: false)
    @State
    private var hoverState: HoverState = .init()
    
    let link: String
    let needsTopPadding: Bool
    let onClickClose: () -> Void
    
    var body: some View {
        VStack(alignment: .leading) {
            toolbarContent
            Spacer()
            textFieldContent
            bottomButtonsContent
            Spacer()
        }
        .padding(.horizontal)
        .padding(.bottom)
        .padding(.top, needsTopPadding ? 16 : 0)
        .onAppear {
            /// MARK: BOOKMARK URL CHANGE LISTENER INITIALIZER
            creationState.listenUrlChanges { url in
                Task { await creationState.fetchData(with: url) }
            }
            Task {
                await creationState.simpleOnAppear(
                    link: link,
                    bookmarkToEdit: nil,
                    collectionToFill: nil,
                    using: modelContext
                )
            }
        }
    }
    
    @ViewBuilder
    private var toolbarContent: some View {
        HStack {
            HStack {
                Text(LocalizedStringKey("Create Bookmark Title"))
                    .font(.title)
                    .fontWeight(.semibold)
                ProgressView()
                    .scaleEffect(0.5)
                    .opacity(creationState.isLoading ? 1 : 0)
                    .animation(.smooth, value: creationState.isLoading)
            }
            Spacer()
            createIcon(for: "cancel-circle")
                .foregroundStyle(
                    hoverState.isHoveredOnCancel
                    ? .red
                    : .white
                )
                .background {
                    Circle()
                        .fill(
                            hoverState.isHoveredOnCancel
                            ? .red.opacity(0.2)
                            : .clear
                        )
                        .frame(width: 22, height: 22)
                }
                .contentShape(Circle())
                .onTapGesture {
                    onClickClose()
                }
                .onHover { isHovered in
                    withAnimation {
                        hoverState.isHoveredOnCancel = isHovered
                    }
                }
        }
        .padding(.top, 4)
        .padding(.bottom)
    }
    
    @ViewBuilder
    private var textFieldContent: some View {
        VStack(spacing: 0) {
            TextField(
                "",
                text: $creationState.url,
                prompt: Text(LocalizedStringKey("Create Bookmark URL Placeholder"))
            )
            .disabled(creationState.isLoading)
            .frame(width: 275)
            .safeAreaInset(edge: .leading) {
                createIcon(for: "link-02")
                    .foregroundStyle(.white)
            }
            HStack {
                if creationState.cleanerUrl.isEmpty {
                    Text(LocalizedStringKey("Create Bookmark Cleaned URL Placeholder"))
                        .foregroundStyle(.tertiary)
                        .safeAreaInset(edge: .leading) {
                            createIcon(for: "clean")
                                .foregroundStyle(.white)
                        }
                } else {
                    Text(creationState.cleanerUrl)
                        .safeAreaInset(edge: .leading) {
                            createIcon(for: "clean")
                                .foregroundStyle(.white)
                                .padding(.trailing, 4)
                        }
                }
                Spacer()
            }
        }.padding(.bottom, 4)
        TextField(
            "",
            text: $creationState.label,
            prompt: Text(LocalizedStringKey("Create Bookmark Title Placeholder"))
        )
        .disabled(creationState.isLoading)
        .frame(width: 275)
        .safeAreaInset(edge: .leading) {
            createIcon(for: "text")
                .foregroundStyle(.white)
        }
    }
    
    @ViewBuilder
    private var bottomButtonsContent: some View {
        HStack {
            Button {
                if let urlEncoded = creationState.url.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
                   let url = URL(string: "yaba://save?link=\(urlEncoded)") {
                    NSWorkspace.shared.open(url)
                    onClickClose()
                }
            } label: {
                Label {
                    Text("Open App Label")
                        .foregroundStyle(.white)
                } icon: {
                    createIcon(for: "bookmark-add-02")
                        .frame(width: 8, height: 8)
                        .foregroundStyle(.white)
                }
                .padding(8)
                .padding(.horizontal, 8)
                .background {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.white.opacity(hoverState.isHoveredOnOpenApp ? 0.2 : 0.1))
                }
            }
            .buttonStyle(.plain)
            .onHover { isHovered in
                withAnimation {
                    hoverState.isHoveredOnOpenApp = isHovered
                }
            }
            Button {
                creationState.onDone(
                    bookmarkToEdit: nil,
                    using: modelContext,
                    onFinishCallback: onClickClose,
                    saveToArchiveOrg: saveToArchiveOrg
                )
            } label: {
                Text(LocalizedStringKey("Save"))
                    .foregroundStyle(.green)
                    .padding(8)
                    .padding(.horizontal, 8)
                    .background {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(.green.opacity(hoverState.isHoveredOnSave ? 0.2 : 0.1))
                    }
            }
            .buttonStyle(.plain)
            .disabled(creationState.url.isEmpty || creationState.label.isEmpty || creationState.isLoading)
            .onHover { isHovered in
                withAnimation {
                    hoverState.isHoveredOnSave = isHovered
                }
            }
        }.padding(.vertical)
    }
    
    @ViewBuilder
    private func createIcon(for resource: String) -> some View {
        Image(resource)
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: 20, height: 20)
    }
}

#Preview {
    SimpleBookmarkCreationMacOSView(
        link: "",
        needsTopPadding: false,
        onClickClose: {}
    )
}
