//
//  SelectTagsContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftData
import SwiftUI

struct SelectTagsContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var machine = TagSelectionStateMachine()

    @State
    private var showCreateTagSheet = false

    @Query(sort: [SortDescriptor(\TagModel.label)])
    private var allTags: [TagModel]

    let initialTagIds: [String]
    let onDone: ([String]) -> Void

    private var trimmedSearch: String {
        machine.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var excludedIdsSorted: [String] {
        Array(machine.state.selectedTagIds).sorted()
    }

    /// Bumps the inner `@Query` when search or selection changes (same idea as `SelectFolderContent`).
    private var selectableQueryIdentity: String {
        trimmedSearch + "\u{1e}" + excludedIdsSorted.joined(separator: "\u{1f}")
    }

    private var selectedTagModelsForDisplay: [TagModel] {
        let selected = machine.state.selectedTagIds
        return allTags
            .filter { selected.contains($0.tagId) }
            .sorted {
                $0.label.localizedStandardCompare($1.label) == .orderedAscending
            }
    }

    var body: some View {
        List {
            Section {
                if machine.state.selectedTagIds.isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("Select Tags No Tags Selected Title")
                        } icon: {
                            YabaIconView(bundleKey: "tags")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text(
                            LocalizedStringKey("Select Tags No Tags Selected Message")
                        )
                    }
                } else {
                    ForEach(selectedTagModelsForDisplay, id: \.tagId) { tag in
                        HStack {
                            YabaIconView(bundleKey: tag.icon)
                                .scaledToFit()
                                .foregroundStyle(tag.color.getUIColor())
                                .frame(width: 24, height: 24)
                            Text(tag.label)
                            Spacer()
                        }
                        .contentShape(Rectangle())
                        .opacity(Constants.Tag.isSystemTag(tag.tagId) ? 0.55 : 1)
                        .onTapGesture {
                            guard !Constants.Tag.isSystemTag(tag.tagId) else { return }
                            Task { @MainActor in
                                await machine.send(.onDeselectTag(tagId: tag.tagId))
                            }
                        }
                    }
                }
            } header: {
                Label {
                    Text("Selected Tags Label Title")
                } icon: {
                    YabaIconView(bundleKey: "checkmark-badge-02")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }

            Section {
                SelectTagsSelectableQueryList(
                    excludedSortedIds: excludedIdsSorted,
                    trimmedSearch: trimmedSearch,
                    selectedIsEmpty: machine.state.selectedTagIds.isEmpty,
                    onAdd: { tag in
                        Task { @MainActor in
                            await machine.send(.onSelectTag(tagId: tag.tagId))
                        }
                    }
                )
                .id(selectableQueryIdentity)
            } header: {
                Label {
                    Text("Selectable Tags Label Title")
                } icon: {
                    YabaIconView(bundleKey: "tag-01")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
            }
        }
        .listRowSpacing(0)
        .scrollDismissesKeyboard(.immediately)
        .searchable(
            text: Binding(
                get: { machine.state.searchQuery },
                set: { newValue in
                    Task {
                        await machine.send(.onSearchQueryChanged(newValue))
                    }
                }
            ),
            prompt: Text("Tags Search Prompt")
        )
        .navigationTitle("Select Tags Title")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }
                .buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreateTagSheet = true
                } label: {
                    YabaIconView(bundleKey: "add-01")
                        .scaledToFit()
                        .frame(width: 22, height: 22)
                }
                .accessibilityLabel(Text("Create Tag Title"))
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    let ids = Array(machine.state.selectedTagIds).sorted()
                    onDone(ids)
                    dismiss()
                } label: {
                    Text("Done")
                }
            }
        }
        .task {
            await machine.send(.onInit(selectedTagIds: initialTagIds))
        }
        .sheet(isPresented: $showCreateTagSheet) {
            TagCreationContent()
        }
    }
}

// MARK: - Query-backed selectable list

/// Lists tags eligible for multi-select using SwiftData filtering (not a full in-memory scan).
private struct SelectTagsSelectableQueryList: View {
    @Query
    private var selectableTags: [TagModel]

    let trimmedSearch: String
    let selectedIsEmpty: Bool
    let onAdd: (TagModel) -> Void

    init(
        excludedSortedIds: [String],
        trimmedSearch: String,
        selectedIsEmpty: Bool,
        onAdd: @escaping (TagModel) -> Void
    ) {
        self.trimmedSearch = trimmedSearch
        self.selectedIsEmpty = selectedIsEmpty
        self.onAdd = onAdd

        let trimmed = trimmedSearch
        let excluded = excludedSortedIds
        let pinnedId = Constants.Tag.Pinned.id
        let privateId = Constants.Tag.Private.id

        _selectableTags = Query(
            filter: #Predicate<TagModel> { tag in
                !tag.isHidden
                    && tag.tagId != pinnedId
                    && tag.tagId != privateId
                    && !excluded.contains(tag.tagId)
                    && (trimmed.isEmpty || tag.label.localizedStandardContains(trimmed))
            },
            sort: [SortDescriptor(\.label)],
            animation: .smooth
        )
    }

    var body: some View {
        Group {
            if selectableTags.isEmpty {
                if trimmedSearch.isEmpty {
                    if selectedIsEmpty {
                        ContentUnavailableView {
                            Label {
                                Text("Select Tags No Tags Available Title")
                            } icon: {
                                YabaIconView(bundleKey: "tag-01")
                                    .scaledToFit()
                                    .frame(width: 52, height: 52)
                            }
                        } description: {
                            Text(
                                LocalizedStringKey(
                                    "Select Tags No Tags Available Description"
                                )
                            )
                        }
                    } else {
                        ContentUnavailableView {
                            Label {
                                Text("Select Tags No More Tags Left Title")
                            } icon: {
                                YabaIconView(bundleKey: "tags")
                                    .scaledToFit()
                                    .frame(width: 52, height: 52)
                            }
                        } description: {
                            Text(
                                LocalizedStringKey(
                                    "Select Tags No More Tags Left Description"
                                )
                            )
                        }
                    }
                } else {
                    ContentUnavailableView {
                        Label {
                            Text("Select Tags No Tags Found In Search Title")
                        } icon: {
                            YabaIconView(bundleKey: "search-01")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text(
                            LocalizedStringKey(
                                "Select Tags No Tags Found In Search Description \(trimmedSearch)"
                            )
                        )
                    }
                }
            } else {
                ForEach(selectableTags, id: \.tagId) { tag in
                    HStack {
                        YabaIconView(bundleKey: tag.icon)
                            .scaledToFit()
                            .foregroundStyle(tag.color.getUIColor())
                            .frame(width: 24, height: 24)
                        Text(tag.label)
                        Spacer()
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onAdd(tag)
                    }
                }
            }
        }
    }
}
