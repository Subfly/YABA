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
    @AppStorage(Constants.preferredCollectionSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    private let collectionType: CollectionType
    private let labelTitle: String
    private let noCollectionsTitle: String
    private let noCollectionsMessage: String
    private let collectionIcon: String
    
    init(collectionType: CollectionType) {
        self.collectionType = collectionType
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
        switch collectionType {
        case .folder:
            FolderView(
                title: labelTitle,
                icon: collectionIcon,
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                noCollectionView: { noCollectionsContent }
            )
        case .tag:
            TagView(
                title: labelTitle,
                icon: collectionIcon,
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                noCollectionView: { noCollectionsContent }
            )
        }
    }
    
    @ViewBuilder
    private var noCollectionsContent: some View {
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

private struct FolderView<NoCollectionView: View>: View {
    @State
    private var isExpanded: Bool = true
    
    @Query
    private var folders: [YabaFolder]
    
    @ViewBuilder
    let noCollectionView: NoCollectionView
    
    let title: String
    let icon: String
    
    init(
        title: String,
        icon: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        @ViewBuilder noCollectionView: () -> NoCollectionView,
    ) {
        self.title = title
        self.icon = icon
        self.noCollectionView = noCollectionView()

        let sortDescriptor: SortDescriptor<YabaFolder> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
        }
        
        _folders = Query(
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
    
    var body: some View {
        Section(isExpanded: $isExpanded) {
            if folders.isEmpty {
                noCollectionView
            } else {
                ForEach(folders) { folder in
                    Text(folder.label)
                }
            }
        } header: {
            Label {
                Text(LocalizedStringKey(title))
            } icon: {
                YabaIconView(bundleKey: icon)
                    .frame(width: 22, height: 22)
            }
        }
    }
}


private struct TagView<NoCollectionView: View>: View {
    @State
    private var isExpanded: Bool = true
    
    @Query
    private var tags: [YabaTag]
    
    @ViewBuilder
    let noCollectionView: NoCollectionView
    
    let title: String
    let icon: String
    
    init(
        title: String,
        icon: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        @ViewBuilder noCollectionView: () -> NoCollectionView,
    ) {
        self.title = title
        self.icon = icon
        self.noCollectionView = noCollectionView()

        let sortDescriptor: SortDescriptor<YabaTag> = switch preferredSorting {
        case .createdAt:
                .init(\.createdAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .editedAt:
                .init(\.editedAt, order: preferredOrder == .ascending ? .forward : .reverse)
        case .label:
                .init(\.label, order: preferredOrder == .ascending ? .forward : .reverse)
        }
        
        _tags = Query(
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
    
    var body: some View {
        Section(isExpanded: $isExpanded) {
            if tags.isEmpty {
                noCollectionView
            } else {
                ForEach(tags) { tag in
                    Text(tag.label)
                }
            }
        } header: {
            Label {
                Text(LocalizedStringKey(title))
            } icon: {
                YabaIconView(bundleKey: icon)
                    .frame(width: 22, height: 22)
            }
        }
    }
}
