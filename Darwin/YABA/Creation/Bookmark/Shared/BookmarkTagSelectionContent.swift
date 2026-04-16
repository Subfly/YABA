//
//  BookmarkTagSelectionContent.swift
//  YABA
//
//  Created by Ali Taha on 16.04.2026.
//

import SwiftData
import SwiftUI

struct BookmarkTagSelectionContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var machine = TagSelectionStateMachine()

    let initialTagIds: [String]
    let onDone: ([String]) -> Void

    @Query(sort: [SortDescriptor(\TagModel.label)])
    private var allTags: [TagModel]

    var body: some View {
        let q = machine.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let visible = allTags.filter { tag in
            !tag.isHidden
                && (q.isEmpty || tag.label.localizedStandardContains(q))
        }

        List {
            Section {
                if visible.isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("Select Tags No Tags Found In Search Title")
                        } icon: {
                            YabaIconView(bundleKey: "search-01")
                                .scaledToFit()
                                .frame(width: 40, height: 40)
                        }
                    }
                } else {
                    ForEach(visible, id: \.tagId) { tag in
                        let selected = machine.state.selectedTagIds.contains(tag.tagId)
                        Button {
                            Task {
                                if selected {
                                    await machine.send(.onDeselectTag(tagId: tag.tagId))
                                } else {
                                    await machine.send(.onSelectTag(tagId: tag.tagId))
                                }
                            }
                        } label: {
                            HStack {
                                YabaIconView(bundleKey: tag.icon)
                                    .scaledToFit()
                                    .foregroundStyle(tag.color.getUIColor())
                                    .frame(width: 24, height: 24)
                                Text(tag.label)
                                Spacer()
                                if selected {
                                    YabaIconView(bundleKey: "checkmark-circle-01")
                                        .foregroundStyle(.tint)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listRowSpacing(0)
        #if !os(visionOS)
        .scrollDismissesKeyboard(.immediately)
        #endif
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
            ToolbarItem(placement: .cancellationAction) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }
                .buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .confirmationAction) {
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
    }
}
