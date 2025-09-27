//
//  SelectImageContent.swift
//  YABA
//
//  Created by Ali Taha on 26.09.2025.
//

import SwiftUI

struct SelectImageContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    // Private internal state for selected image
    @State
    private var currentSelected: String? = nil
    
    private let columns: [GridItem] = if UIDevice.current.userInterfaceIdiom == .pad {
        [.init(.flexible()), .init(.flexible()), .init(.flexible())]
    } else {
        [.init(.flexible()), .init(.flexible())]
    }
    
    let selectables: [String: Data]
    let isLoading: Bool
    let onSelectImage: (String, Data) -> Void
    
    var body: some View {
        NavigationView {
            content
                .navigationTitle(Text("Select Image Title"))
                .toolbar {
                    ToolbarItem(placement: .navigation) {
                        Button {
                            dismiss()
                        } label: {
                            Text("Cancel")
                        }
                    }
                    ToolbarItem(placement: .primaryAction) {
                        Button {
                            if let currentSelected,
                               let imageData = selectables[currentSelected] {
                                onSelectImage(currentSelected, imageData)
                                // Button should be disabled anyway,
                                // so dismiss should only happen if
                                // a selection is successfull
                                dismiss()
                            }
                        } label: {
                            Text("Done")
                        }.disabled(currentSelected == nil)
                    }
                }
        }
    }
    
    @ViewBuilder
    private var content: some View {
        if isLoading {
            ProgressView()
        } else if selectables.isEmpty {
            ContentUnavailableView {
                Label {
                    Text("Select Image No Image Found Title")
                } icon: {
                    YabaIconView(bundleKey: "image-not-found-01")
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                        .padding(.top)
                }
            } description: {
                Text("Select Image No Image Found Message")
            }
        } else {
            ScrollView {
                LazyVGrid(columns: columns) {
                    ForEach(selectables.keys.sorted(), id: \.self) { url in
                        if let imageData = selectables[url],
                           let uiImage = UIImage(data: imageData) {
                            SelectableImage(
                                uiImage: uiImage,
                                isSelected: currentSelected == url,
                                onSelected: {
                                    if currentSelected == url {
                                        currentSelected = nil
                                    } else {
                                        currentSelected = url
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private struct SelectableImage: View {
    let uiImage: UIImage
    let isSelected: Bool
    let onSelected: () -> Void
    
    var body: some View {
        Image(uiImage: uiImage).resizable()
            .aspectRatio(1, contentMode: .fill)
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay {
                if isSelected {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(.secondary.opacity(0.5))
                        .overlay {
                            YabaIconView(bundleKey: "checkmark-circle-02")
                                .frame(width: 48, height: 48)
                                .foregroundStyle(.tint)
                        }
                }
            }
            .onTapGesture {
                onSelected()
            }
    }
}

#Preview {
    SelectImageContent(
        selectables: [:],
        isLoading: false,
        onSelectImage: { _, _ in }
    )
}
