//
//  SelectTagsContent.swift
//  YABA
//
//  Created by Ali Taha on 22.04.2025.
//

import SwiftUI
import SwiftData

struct SelectTagsContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var showTagCreationSheet: Bool = false
    
    @State
    private var searchQuery: String = ""
    
    @Binding
    var selectedTags: [YabaCollection]
    
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
                    showTagCreationSheet = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    dismiss()
                } label: {
                    Text("Done")
                }
            }
        }
        .sheet(isPresented: $showTagCreationSheet) {
            CollectionCreationContent(
                collectionType: .tag,
                collectionToAdd: nil,
                collectionToEdit: nil,
                onEditCallback: { _ in }
            )
        }
    }
}

private struct SelectTagsSearchableContent: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Query
    private var allTags: [YabaCollection]
    
    @Binding
    var selectedTags: [YabaCollection]
    
    @Binding
    var searchQuery: String
    
    init(
        selectedTags: Binding<[YabaCollection]>,
        searchQuery: Binding<String>
    ) {
        let compareValue = CollectionType.tag.rawValue
        let query = searchQuery.wrappedValue
        _allTags = Query(
            filter: #Predicate<YabaCollection> {
                if query.isEmpty {
                    $0.type == compareValue
                } else {
                    $0.type == compareValue
                    && $0.label.localizedStandardContains(query)
                }
            }
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
                        CollectionItemView(
                            collection: tag,
                            // Tags have their own section to indicate tag is already selected
                            inSelectionModeAndSelected: false,
                            isInCreationMode: true,
                            isInBookmarkDetail: false,
                            onDeleteCallback: { collection in
                                withAnimation {
                                    if let indexOfTag = selectedTags.firstIndex(of: collection) {
                                        selectedTags.remove(at: indexOfTag)
                                    }
                                }
                            },
                            onEditCallback: { collection in
                                withAnimation {
                                    if let indexOfTag = selectedTags.firstIndex(of: collection) {
                                        selectedTags[indexOfTag] = collection
                                    }
                                }
                            },
                            onNavigationCallback: { _ in }
                        )
                        .contentShape(Rectangle())
                        .onTapGesture {
                            withAnimation {
                                if let indexOfTag = selectedTags.firstIndex(of: tag) {
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
                // Thanks swiftdata for this...
                let tags = allTags.filter({ !selectedTags.contains($0) })
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
                        CollectionItemView(
                            collection: tag,
                            inSelectionModeAndSelected: true,
                            isInCreationMode: true,
                            isInBookmarkDetail: false,
                            onDeleteCallback: { _ in },
                            onEditCallback: { _ in },
                            onNavigationCallback: { _ in }
                        )
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
        }.listRowSpacing(0)
    }
}

#Preview {
    SelectTagsContent(selectedTags: .constant([]))
}
