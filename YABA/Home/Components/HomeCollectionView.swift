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
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var contentAppearance: ViewType = .list
    
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
        selectedSorting: SortType,
        selectedSortOrder: SortOrderType,
        onNavigationCallback: @escaping (YabaCollection) -> Void
    ) {
        _isExpanded = isExpanded
        _selectedCollection = selectedCollection
        
        let sortDescriptor: SortDescriptor<YabaCollection> = switch selectedSorting {
        case .createdAt:
                .init(\.createdAt, order: selectedSortOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: selectedSortOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: selectedSortOrder == .ascending ? .forward : .reverse)
        }
        
        _collections = Query(
            filter: #Predicate<YabaCollection> {
                $0.type == collectionType.rawValue
            },
            sort: [sortDescriptor],
            animation: .smooth
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
        listSection
    }
    
    @ViewBuilder
    private var listSection: some View {
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
    private var gridSection: some View {
        // Worst hack for full span items, thanks SwiftUI
        Section {} header: {
            HStack {
                HStack {
                    YabaIconView(bundleKey: collectionIcon)
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                        .foregroundStyle(.secondary)
                    Text(LocalizedStringKey(labelTitle))
                        .textCase(.uppercase)
                        .font(.headline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                #if targetEnvironment(macCatalyst)
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                    .foregroundStyle(.tint)
                    .rotationEffect(isExpanded ? .degrees(90) : .degrees(0))
                    .animation(.smooth, value: isExpanded)
                #endif
            }
            .padding(.leading)
            .contentShape(Rectangle())
            #if targetEnvironment(macCatalyst)
            .onTapGesture { isExpanded.toggle() }
            #endif
        }
        gridInnerContent
            .animation(.smooth, value: isExpanded)
            .transition(.blurReplace)
    }
    
    @ViewBuilder
    private var gridInnerContent: some View {
        #if targetEnvironment(macCatalyst)
        if isExpanded {
            if collections.isEmpty {
                Section {} header: { noCollectionsView }
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
        }
        #else
        if collections.isEmpty {
            Section {} header: { noCollectionsView }
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
        #endif
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
        selectedSorting: .createdAt,
        selectedSortOrder: .ascending,
        onNavigationCallback: { _ in }
    )
}

#Preview {
    HomeCollectionView(
        collectionType: .tag,
        isExpanded: .constant(true),
        selectedCollection: .constant(.empty()),
        selectedSorting: .createdAt,
        selectedSortOrder: .ascending,
        onNavigationCallback: { _ in }
    )
}
