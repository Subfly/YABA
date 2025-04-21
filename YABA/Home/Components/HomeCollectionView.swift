//
//  HomeCollectionView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

struct HomeCollectionView: View {
    @Query
    private var collections: [YabaCollection]
    
    @Binding
    var isExpanded: Bool
    
    @Binding
    var selectedCollection: YabaCollection?
    
    private let labelTitle: String
    private let noCollectionsTitle: String
    private let noCollectionsMessage: String
    private let collectionIcon: String
    
    init(
        collectionType: CollectionType,
        isExpanded: Binding<Bool>,
        selectedCollection: Binding<YabaCollection?>
    ) {
        _isExpanded = isExpanded
        _selectedCollection = selectedCollection
        _collections = Query(
            filter: #Predicate<YabaCollection> {
                $0.type == collectionType.rawValue
            }
        )
        
        switch collectionType {
        case .folder:
            labelTitle = "Folders Title"
            noCollectionsTitle = "No Folders Title"
            noCollectionsMessage = "No Folders Message"
            collectionIcon = "folder"
        case .tag:
            labelTitle = "Tags Title"
            noCollectionsTitle = "No Tags Title"
            noCollectionsMessage = "No Tags Message"
            collectionIcon = "tag"
        }
    }
    
    var body: some View {
        Section(isExpanded: $isExpanded) {
            if collections.isEmpty {
                noCollectionsView
            } else {
                ForEach(collections) { collection in
                    CollectionItemView(
                        collection: collection,
                        selectedCollection: $selectedCollection
                    )
                    #if os(macOS)
                    .listRowBackground(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(
                                collection.id == selectedCollection?.id
                                ? collection.color.getUIColor().opacity(0.2)
                                : Color.clear
                            )
                            .padding(.horizontal, 10)
                    )
                    #endif
                }
            }
        } header: {
            Label(LocalizedStringKey(labelTitle), systemImage: collectionIcon)
                .font(.headline)
        }
    }
    
    @ViewBuilder
    private var noCollectionsView: some View {
        #if os(macOS)
        Text(LocalizedStringKey(noCollectionsTitle))
            .font(.caption)
            .foregroundStyle(.secondary)
        #else
        ContentUnavailableView {
            Label(LocalizedStringKey(noCollectionsTitle), systemImage: collectionIcon)
        } description: {
            Text(LocalizedStringKey(noCollectionsMessage))
        }
        #endif
    }
}

#Preview {
    HomeCollectionView(
        collectionType: .folder,
        isExpanded: .constant(true),
        selectedCollection: .constant(.empty())
    )
}

#Preview {
    HomeCollectionView(
        collectionType: .tag,
        isExpanded: .constant(true),
        selectedCollection: .constant(.empty())
    )
}
