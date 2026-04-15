//
//  SelectTagsContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftData
import SwiftUI

#if false

struct SelectTagsContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @State
    private var searchQuery: String = ""

    @Binding
    var selectedTags: [TagModel]

    var body: some View {
        SelectTagsSearchableContent(
            selectedTags: $selectedTags,
            searchQuery: $searchQuery
        )
        #if !os(visionOS)
        .scrollDismissesKeyboard(.immediately)
        #endif
        .searchable(text: $searchQuery, prompt: "Tags Search Prompt")
        .navigationTitle("Select Tags Title")
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    dismiss()
                } label: {
                    YabaIconView(bundleKey: "arrow-left-01")
                }.buttonRepeatBehavior(.enabled)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    // Tag creation UI archived (`YABA/Creation/Collection`).
                } label: {
                    Image(systemName: "plus")
                }
                .disabled(true)
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    dismiss()
                } label: {
                    Text("Done")
                }
            }
        }
    }
}

private struct SelectTagsSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss

    @Query
    private var allTags: [TagModel]

    @Binding
    var selectedTags: [TagModel]

    @Binding
    var searchQuery: String

    init(
        selectedTags: Binding<[TagModel]>,
        searchQuery: Binding<String>
    ) {
        let query = searchQuery.wrappedValue
        _allTags = Query(
            filter: #Predicate<TagModel> {
                query.isEmpty || $0.label.localizedStandardContains(query)
            },
            sort: [SortDescriptor(\.label)],
            animation: .smooth
        )
        _selectedTags = selectedTags
        _searchQuery = searchQuery
    }

    var body: some View {
        List {
            Section {
                if selectedTags.isEmpty {
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
                    ForEach(selectedTags) { tag in
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
                            withAnimation {
                                if let indexOfTag = selectedTags.firstIndex(where: { $0.tagId == tag.tagId }) {
                                    selectedTags.remove(at: indexOfTag)
                                }
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
                let tags = allTags.filter { candidate in
                    !selectedTags.contains(where: { $0.tagId == candidate.tagId })
                }
                if tags.isEmpty {
                    if searchQuery.isEmpty {
                        if selectedTags.isEmpty {
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
                                    "Select Tags No Tags Found In Search Description \(searchQuery)"
                                )
                            )
                        }
                    }
                } else {
                    ForEach(tags) { tag in
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
                            withAnimation {
                                selectedTags.append(tag)
                            }
                        }
                    }
                }
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
    }
}

#Preview {
    SelectTagsContent(selectedTags: .constant([]))
}

#endif
