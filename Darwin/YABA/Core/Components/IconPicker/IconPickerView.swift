//
//  IconPickerView.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

/// Two-step flow aligned with Compose: `IconCategorySelectionContent` → `IconSelectionContent`,
/// driven by `IconCategorySelectionStateMachine` and `IconSelectionStateMachine` (YABACore).
struct IconPickerView: View {
    let currentSelectedIcon: String
    let onSelectIcon: (String) -> Void
    let onCancel: () -> Void

    @State
    private var categoryMachine = IconCategorySelectionStateMachine()
    @State
    private var iconMachine = IconSelectionStateMachine()

    init(
        currentSelectedIcon: String = "",
        onSelectIcon: @escaping (String) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.currentSelectedIcon = currentSelectedIcon
        self.onSelectIcon = onSelectIcon
        self.onCancel = onCancel
    }

    var body: some View {
        NavigationStack {
            categoryList
                .navigationDestination(for: YabaIconCategory.self) { category in
                    iconSelectionContent(for: category)
                }
        }
    }

    private var categoryList: some View {
        Group {
            if categoryMachine.state.isLoading, categoryMachine.state.categories.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(categoryMachine.state.categories) { category in
                    NavigationLink(value: category) {
                        IconCategoryRow(category: category)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle(Text("Pick Icon Category Title"))
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", role: .cancel) {
                    onCancel()
                }
            }
        }
        .task {
            await categoryMachine.send(.onInit)
        }
    }

    private func iconSelectionContent(for category: YabaIconCategory) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                if iconMachine.state.isLoadingIcons {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                LazyVGrid(columns: iconGridColumns, spacing: 8) {
                    ForEach(iconMachine.state.icons, id: \.name) { icon in
                        IconGridCell(
                            iconName: icon.name,
                            isSelected: iconMachine.state.selectedIconName == icon.name
                        ) {
                            Task {
                                await iconMachine.send(.onSelectIcon(iconName: icon.name))
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 24)
            }
        }
        .navigationTitle(Text(LocalizedStringKey(category.name)))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") {
                    onSelectIcon(iconMachine.state.selectedIconName)
                }
            }
        }
        .task(id: category.id) {
            await iconMachine.send(
                .onInit(category: category, initialSelectedIcon: currentSelectedIcon)
            )
        }
    }

    private var iconGridColumns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 8), count: 5)
    }
}

private struct IconCategoryRow: View {
    let category: YabaIconCategory

    var body: some View {
        HStack(spacing: 12) {
            YabaIconView(bundleKey: category.headerIcon)
                .scaledToFit()
                .foregroundStyle(YabaColor(rawValue: category.color)?.getUIColor() ?? .accentColor)
                .frame(width: 28, height: 28)
            Text(LocalizedStringKey(category.name))
                .foregroundStyle(.primary)
            Spacer()
            Text("\(category.iconCount)")
                .foregroundStyle(.secondary)
                .fontWeight(.medium)
            YabaIconView(bundleKey: "arrow-right-01")
                .scaledToFit()
                .frame(width: 20, height: 20)
                .foregroundStyle(.tertiary)
        }
        .contentShape(Rectangle())
    }
}

private struct IconGridCell: View {
    let iconName: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            YabaIconView(bundleKey: iconName)
                .aspectRatio(contentMode: .fit)
                .frame(width: 40, height: 40)
                .frame(maxWidth: .infinity)
                .padding(8)
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(isSelected ? Color.accentColor.opacity(0.35) : Color.clear)
                }
        }
        .buttonStyle(.plain)
    }
}
