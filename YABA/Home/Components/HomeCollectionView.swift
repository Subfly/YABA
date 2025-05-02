//
//  HomeCollectionView.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import SwiftUI
import SwiftData

@MainActor
struct HomeCollectionView: View {
    @Query
    private var collections: [YabaCollection]
    
    @Binding
    var isExpanded: Bool
    
    @Binding
    var selectedCollection: YabaCollection?
    
    let onNavigationCallback: (YabaCollection) -> Void
    
    private let labelTitle: String
    private let noCollectionsTitle: String
    private let noCollectionsMessage: String
    private let collectionIcon: String
    
    init(
        collectionType: CollectionType,
        isExpanded: Binding<Bool>,
        selectedCollection: Binding<YabaCollection?>,
        onNavigationCallback: @escaping (YabaCollection) -> Void
    ) {
        _isExpanded = isExpanded
        _selectedCollection = selectedCollection
        _collections = Query(
            filter: #Predicate<YabaCollection> {
                $0.type == collectionType.rawValue
            }
        )
        self.onNavigationCallback = onNavigationCallback
        
        switch collectionType {
        case .folder:
            labelTitle = "Folders Title"
            noCollectionsTitle = "No Folders Title"
            noCollectionsMessage = "No Folders Message"
            collectionIcon = "folder-01"
        case .tag:
            labelTitle = "Tags Title"
            noCollectionsTitle = "No Tags Title"
            noCollectionsMessage = "No Tags Message"
            collectionIcon = "tag-01"
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
                        selectedCollection: $selectedCollection,
                        isInSelectionMode: false,
                        isInBookmarkDetail: false,
                        onDeleteCallback: { _ in },
                        onEditCallback: { _ in },
                        onNavigationCallback: onNavigationCallback
                    )
                }
            }
        } header: {
            Label {
                Text(LocalizedStringKey(labelTitle))
                #if targetEnvironment(macCatalyst)
                    .font(.headline)
                #endif
            } icon: {
                YabaIconView(bundleKey: collectionIcon)
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var noCollectionsView: some View {
        ContentUnavailableView {
            Label {
                Text(LocalizedStringKey(noCollectionsTitle))
            } icon: {
                YabaIconView(bundleKey: collectionIcon)
                    .scaledToFit()
                    .frame(width: 52, height: 52)
            }
        } description: {
            Text(LocalizedStringKey(noCollectionsMessage))
        }
    }
}

#Preview {
    HomeCollectionView(
        collectionType: .folder,
        isExpanded: .constant(true),
        selectedCollection: .constant(.empty()),
        onNavigationCallback: { _ in }
    )
}

#Preview {
    HomeCollectionView(
        collectionType: .tag,
        isExpanded: .constant(true),
        selectedCollection: .constant(.empty()),
        onNavigationCallback: { _ in }
    )
}
