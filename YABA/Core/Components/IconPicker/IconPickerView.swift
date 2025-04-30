//
//  IconPickerView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

struct IconPickerView: View {
    @State
    private var searchQuery: String = ""
    
    @State
    private var data: IconPickerData = .init()
    
    private let columns = [
        GridItem(),
        GridItem(),
        GridItem(),
        GridItem()
    ]
    
    let onSelectIcon: (String) -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 32) {
                    ForEach(data.iconKeys, id: \.self) { iconKey in
                        let subIcons = data.getIconValues(for: iconKey)
                        PickableIcon(
                            key: iconKey,
                            subIcons: subIcons,
                            onSelectIcon: onSelectIcon
                        )
                    }
                }
                .safeAreaPadding(.top)
            }
            .searchable(text: $searchQuery, prompt: "Search Icons")
            .onChange(of: searchQuery) { _, newQuery in
                withAnimation {
                    data.onQueryChange(newQuery)
                }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        onCancel()
                    }
                }
            }
            .navigationTitle("Pick Icon")
        }
    }
}

private struct PickableIcon: View {
    let key: String
    let subIcons: [String]
    let onSelectIcon: (String) -> Void
    
    @State
    private var showVariantSelectionSheet: Bool = false
    
    private let columns = [
        GridItem(),
        GridItem(),
        GridItem(),
        GridItem()
    ]
    
    var body: some View {
        Image(systemName: subIcons.first ?? key)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 44, height: 44)
            .onTapGesture {
                if subIcons.count == 1 {
                    onSelectIcon(key)
                } else {
                    showVariantSelectionSheet = true
                }
            }
            .sheet(isPresented: $showVariantSelectionSheet) {
                NavigationView {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 32) {
                            ForEach(subIcons, id: \.self) { iconKey in
                                Image(systemName: iconKey)
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 44, height: 44)
                                    .onTapGesture {
                                        onSelectIcon(iconKey)
                                    }
                            }
                        }
                        .safeAreaPadding(.top)
                    }
                    .navigationTitle("Pick Icon Variant")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel", role: .cancel) {
                                showVariantSelectionSheet = false
                            }
                        }
                    }
                }.presentationDetents([.fraction(subIcons.count > 4 ? 0.5 : 0.25)])
            }
    }
}

#Preview {
    IconPickerView(onSelectIcon: { _ in }, onCancel: {})
}
