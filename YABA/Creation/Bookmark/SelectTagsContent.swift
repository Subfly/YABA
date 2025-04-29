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
        #if os(iOS)
        iOSView
        #elseif os(macOS)
        macOSView
        #endif
    }
    
    @ViewBuilder
    private var iOSView: some View {
        SelectTagsSearchableContent(
            selectedTags: $selectedTags,
            searchQuery: $searchQuery
        )
        .scrollDismissesKeyboard(.immediately)
        .searchable(text: $searchQuery, prompt: "Tags Search Prompt")
        .navigationTitle("Select Tags Title")
        .toolbar {
            #if os(iOS)
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showTagCreationSheet = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            #endif
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
                collectionToEdit: .constant(nil),
                onEditCallback: { _ in }
            )
        }
    }
    
    @ViewBuilder
    private var macOSView: some View {
        VStack {
            HStack {
                Text("Select Tags Title")
                    .font(.title3)
                    .fontWeight(.semibold)
                Spacer()
                TextField(
                    "",
                    text: $searchQuery,
                    prompt: Text("Tags Search Prompt")
                ).frame(width: 125)
                Button {
                    showTagCreationSheet = true
                } label: {
                    Image(systemName: "plus")
                }
            }
            .padding([.top, .leading, .trailing], 12)
            SelectTagsSearchableContent(
                selectedTags: $selectedTags,
                searchQuery: $searchQuery
            )
        }
        .frame(width: 325, height: 400)
        .sheet(isPresented: $showTagCreationSheet) {
            CollectionCreationContent(
                collectionType: .tag,
                collectionToEdit: .constant(nil),
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
                    ContentUnavailableView(
                        "Select Tags No Tags Selected Title",
                        systemImage: "x.circle",
                        description: Text(
                            LocalizedStringKey("Select Tags No Tags Selected Message")
                        )
                    )
                } else {
                    ForEach(selectedTags) { tag in
                        CollectionItemView(
                            collection: tag,
                            selectedCollection: .constant(nil),
                            isInSelectionMode: true,
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
                Label("Selected Tags Label Title", systemImage: "checkmark.circle")
            }
            Section {
                // Thanks swiftdata for this...
                let tags = allTags.filter({ !selectedTags.contains($0) })
                if tags.isEmpty {
                    if searchQuery.isEmpty {
                        if selectedTags.isEmpty {
                            ContentUnavailableView(
                                "Select Tags No Tags Available Title",
                                systemImage: "tag",
                                description: Text(
                                    LocalizedStringKey(
                                        "Select Tags No Tags Available Description"
                                    )
                                )
                            )
                        } else {
                            ContentUnavailableView(
                                "Select Tags No More Tags Left Title",
                                systemImage: "x.circle",
                                description: Text(
                                    LocalizedStringKey(
                                        "Select Tags No More Tags Left Description"
                                    )
                                )
                            )
                        }
                    } else {
                        ContentUnavailableView(
                            "Select Tags No Tags Found In Search Title",
                            systemImage: "magnifyingglass",
                            description: Text(
                                LocalizedStringKey(
                                    "Select Tags No Tags Found In Search Description \(searchQuery)"
                                )
                            )
                        )
                    }
                } else {
                    ForEach(tags) { tag in
                        CollectionItemView(
                            collection: tag,
                            selectedCollection: .constant(nil),
                            isInSelectionMode: true,
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
                Label("Selectable Tags Label Title", systemImage: "tag")
            }
        }
        #if os(macOS)
        .listStyle(.sidebar)
        #endif
    }
}

#Preview {
    SelectTagsContent(selectedTags: .constant([]))
}
