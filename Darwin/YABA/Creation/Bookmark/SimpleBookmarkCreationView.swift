//
//  SimpleBookmarkCreationView.swift
//  YABA
//
//  Created by Ali Taha on 12.08.2025.
//

import SwiftUI

struct SimpleBookmarkCreationView: View {
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var creationState: BookmarkCreationState = .init(isInEditMode: false)
    
    let link: String
    let onExitRequested: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                List {
                    ImageSection(
                        imageData: creationState.imageData,
                        iconData: creationState.iconData,
                        domain: creationState.host,
                        link: creationState.cleanerUrl
                    ).redacted(reason: creationState.isLoading ? .placeholder : [])
                    titleSection
                    infoSection
                }
                .listStyle(.sidebar)
                .scrollContentBackground(.hidden)
            }
            .navigationTitle("Create Bookmark Title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        onExitRequested()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        creationState.onDone(
                            bookmarkToEdit: nil,
                            using: modelContext,
                            onFinishCallback: {
                                onExitRequested()
                                dismiss()
                            }
                        )
                    }.disabled(
                        creationState.url.isEmpty ||
                        creationState.label.isEmpty ||
                        creationState.isLoading
                    )
                }
            }
        }
        .presentationDetents([.large])
        .tint(.accentColor)
        .onAppear {
            /// MARK: BOOKMARK URL CHANGE LISTENER INITIALIZER
            creationState.listenUrlChanges { url in
                Task { await creationState.fetchData(with: url) }
            }
            Task {
                await creationState.onAppear(
                    link: link,
                    bookmarkToEdit: nil,
                    collectionToFill: nil,
                    storedContentAppearance: .list, // Not important
                    storedCardImageSizing: .big, // Not important
                    using: modelContext
                )
            }
        }
        .toast(
            state: creationState.toastManager.toastState,
            isShowing: creationState.toastManager.isShowing,
            onDismiss: {
                creationState.toastManager.hide()
            }
        )
    }
    
    @ViewBuilder
    private var titleSection: some View {
        Section {
            TextField(
                "",
                text: $creationState.label,
                prompt: Text("Create Bookmark Title Placeholder")
            )
            .redacted(reason: creationState.isLoading ? .placeholder : [])
            .safeAreaInset(edge: .leading) {
                YabaIconView(bundleKey: "text")
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.tint)
            }
            .safeAreaInset(edge: .trailing) {
                if !creationState.label.isEmpty {
                    Button {
                        creationState.onClearTitle()
                    } label: {
                        YabaIconView(bundleKey: "cancel-circle")
                            .scaledToFit()
                            .frame(width: 16, height: 16)
                            .foregroundStyle(.tint)
                    }
                }
            }
        } header: {
            Label {
                Text("Bookmark Title Placeholder")
            } icon: {
                YabaIconView(bundleKey: "pencil-edit-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var infoSection: some View {
        let extra = LocalizedStringResource(stringLiteral: "Uncategorized Label")
        Section {
            Text("Simple Bookmark Creation Info Message \(extra)")
        } header: {
            Label {
                Text("Info")
            } icon: {
                YabaIconView(bundleKey: "information-circle")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

private struct ImageSection: View {
    let imageData: Data?
    let iconData: Data?
    let domain: String
    let link: String
    
    var body: some View {
        Section {
            imageContent
        } header: {
            Label {
                Text("Bookmark Detail Image Header Title")
            } icon: {
                YabaIconView(bundleKey: "image-03")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }.padding(.leading)
        } footer: {
            HStack {
                if let iconData,
                   let image = UIImage(data: iconData) {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 18, height: 18)
                } else {
                    YabaIconView(bundleKey: "link-02")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                Text(domain.isEmpty ? link : domain)
                    .lineLimit(2)
            }.padding(.leading)
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
    }
    
    @ViewBuilder
    private var imageContent: some View {
        if let imageData,
           let image = UIImage(data: imageData) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(idealHeight: 250, alignment: .center)
        } else {
            RoundedRectangle(cornerRadius: 12)
                .fill(.tint.opacity(0.3))
                .frame(idealHeight: 250, alignment: .center)
                .overlay {
                    YabaIconView(bundleKey: BookmarkType.none.getIconName())
                        .scaledToFit()
                        .foregroundStyle(.tint)
                        .frame(width: 125, height: 125)
                }
        }
    }
}

#Preview {
    SimpleBookmarkCreationView(
        link: "",
        onExitRequested: {}
    )
}
