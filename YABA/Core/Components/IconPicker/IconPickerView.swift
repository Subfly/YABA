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
                    ForEach(data.icons, id: \.self) { icon in
                        PickableIcon(
                            key: icon.name,
                            onSelectIcon: onSelectIcon
                        )
                    }
                }
                .safeAreaPadding(.top)
            }
            .searchable(
                text: $searchQuery,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: "Search Icons"
            )
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
    @Environment(\.colorScheme)
    private var colorScheme
    
    let key: String
    let onSelectIcon: (String) -> Void
    
    var body: some View {
        YabaIconView(bundleKey: key)
            .aspectRatio(contentMode: .fit)
            .frame(width: 44, height: 44)
            .contentShape(Rectangle())
            .onTapGesture {
                onSelectIcon(key)
            }
    }
}

#Preview {
    IconPickerView(onSelectIcon: { _ in }, onCancel: {})
}
