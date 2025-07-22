//
//  IconPickerView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

struct IconPickerView: View {
    @State
    private var data: IconPickerData = .init()
    
    let onSelectIcon: (String) -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationView {
            List {
                ForEach(data.categories, id: \.self) { category in
                    CategoryItemView(category: category, data: data, onSelectIcon: onSelectIcon)
                }
            }
            .listStyle(.sidebar)
            .listRowSpacing(0)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        onCancel()
                    }
                }
            }
            .navigationTitle("Pick Icon Category Title")
        }
    }
}

private struct CategoryItemView: View {
    let category: IconCategory
    let data: IconPickerData
    let onSelectIcon: (String) -> Void
    
    var body: some View {
        Section {
            ForEach(category.subcategories, id: \.id) { subcategory in
                NavigationLink {
                    IconSelectionView(
                        subcategory: subcategory,
                        data: data,
                        onSelectIcon: onSelectIcon
                    )
                } label: {
                    HStack {
                        HStack {
                            YabaIconView(bundleKey: subcategory.headerIcon)
                                .scaledToFit()
                                .foregroundStyle(YabaColor(rawValue: subcategory.color)?.getUIColor() ?? .accentColor)
                                .frame(width: 24, height: 24)
                            Text(subcategory.name)
                        }
                        Spacer()
                        HStack {
                            Text("\(subcategory.iconCount)")
                                .foregroundStyle(.secondary)
                                .fontWeight(.medium)
                        }.foregroundStyle(.secondary)
                    }
                    .contentShape(Rectangle())
                }
            }
        } header: {
            Label {
                Text(category.name)
            } icon: {
                YabaIconView(bundleKey: category.headerIcon)
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }.foregroundStyle(YabaColor(rawValue: category.color)?.getUIColor() ?? .accentColor)
        } footer: {
            Text(category.description)
        }
    }
}

private struct IconSelectionView: View {
    let subcategory: IconSubcategory
    let data: IconPickerData
    let onSelectIcon: (String) -> Void
    
    private let columns = Array(repeating: GridItem(.adaptive(minimum: 60, maximum: 80), spacing: 12), count: 4)
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 16) {
                ForEach(data.currentIcons, id: \.name) { icon in
                    PickableIcon(
                        key: icon.name,
                        onSelectIcon: onSelectIcon
                    )
                }
            }
            .padding()
        }
        .navigationTitle(subcategory.name)
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            data.loadIcons(for: subcategory)
        }
    }
}

private struct PickableIcon: View {
    @Environment(\.colorScheme)
    private var colorScheme
    
    let key: String
    let onSelectIcon: (String) -> Void
    
    var body: some View {
        Button {
            onSelectIcon(key)
        } label: {
            VStack(spacing: 4) {
                YabaIconView(bundleKey: key)
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 44, height: 44)
                    .foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    IconPickerView(onSelectIcon: { _ in }, onCancel: {})
}
