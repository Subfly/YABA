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
    private let onSelectFolder: (String) -> Void
    private let onSelectTag: (String) -> Void

    init(
        collectionType: CollectionType,
        onSelectFolder: @escaping (String) -> Void = { _ in },
        onSelectTag: @escaping (String) -> Void = { _ in }
    ) {
        self.collectionType = collectionType
        self.onSelectFolder = onSelectFolder
        self.onSelectTag = onSelectTag
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
                onSelectFolder: onSelectFolder,
                noCollectionView: { noCollectionsContent }
            )
        case .tag:
            TagView(
                title: labelTitle,
                icon: collectionIcon,
                preferredSorting: preferredSorting,
                preferredOrder: preferredSortOrder,
                onSelectTag: onSelectTag,
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
    private let preferredSorting: SortType
    private let preferredOrder: SortOrderType
    let onSelectFolder: (String) -> Void

    init(
        title: String,
        icon: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onSelectFolder: @escaping (String) -> Void,
        @ViewBuilder noCollectionView: () -> NoCollectionView,
    ) {
        self.title = title
        self.icon = icon
        self.preferredSorting = preferredSorting
        self.preferredOrder = preferredOrder
        self.onSelectFolder = onSelectFolder
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
            filter: #Predicate<FolderModel> { folder in
                folder.parent == nil
            },
            sort: [sortDescriptor],
            animation: .smooth
        )
    }
    
    var body: some View {
        Section(isExpanded: $isExpanded) {
            if folders.isEmpty {
                noCollectionView
            } else {
                SortedFolderOutlineGroup(
                    folders: Array(folders),
                    sorting: preferredSorting,
                    order: preferredOrder,
                    onSelectFolder: onSelectFolder
                )
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

// MARK: - Folder outline sorting (matches `Query` sort + applies to nested levels)

private struct SortedFolderOutlineGroup: View {
    let folders: [FolderModel]
    let sorting: SortType
    let order: SortOrderType
    let onSelectFolder: (String) -> Void

    var body: some View {
        OutlineGroup(
            sortedFolderModels(folders, sorting: sorting, order: order).map {
                HomeFolderOutlineNode(folder: $0, sorting: sorting, order: order)
            },
            id: \.id,
            children: \.childNodes
        ) { node in
            Button {
                onSelectFolder(node.folder.folderId)
            } label: {
                FolderItemView(folder: node.folder)
            }
            .buttonStyle(.plain)
        }
    }
}


/// Carries the user’s sort preference so `OutlineGroup` children use the same order as root folders.
private struct HomeFolderOutlineNode: Identifiable {
    let id: String
    let folder: FolderModel
    let sorting: SortType
    let order: SortOrderType

    init(folder: FolderModel, sorting: SortType, order: SortOrderType) {
        self.folder = folder
        self.sorting = sorting
        self.order = order
        self.id = folder.folderId
    }

    var childNodes: [HomeFolderOutlineNode]? {
        let list = Array(folder.children)
        guard !list.isEmpty else { return nil }
        let sorted = sortedFolderModels(list, sorting: sorting, order: order)
        return sorted.map { HomeFolderOutlineNode(folder: $0, sorting: sorting, order: order) }
    }
}

private func sortedFolderModels(
    _ folders: [FolderModel],
    sorting: SortType,
    order: SortOrderType,
) -> [FolderModel] {
    let ascending = order == .ascending
    switch sorting {
    case .createdAt:
        return folders.sorted { a, b in
            ascending ? a.createdAt < b.createdAt : a.createdAt > b.createdAt
        }
    case .editedAt:
        return folders.sorted { a, b in
            ascending ? a.editedAt < b.editedAt : a.editedAt > b.editedAt
        }
    case .label:
        return folders.sorted { a, b in
            let cmp = a.label.localizedStandardCompare(b.label)
            switch cmp {
            case .orderedSame:
                return false
            case .orderedAscending:
                return ascending
            case .orderedDescending:
                return !ascending
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
    let onSelectTag: (String) -> Void

    init(
        title: String,
        icon: String,
        preferredSorting: SortType,
        preferredOrder: SortOrderType,
        onSelectTag: @escaping (String) -> Void,
        @ViewBuilder noCollectionView: () -> NoCollectionView,
    ) {
        self.title = title
        self.icon = icon
        self.onSelectTag = onSelectTag
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
                    Button {
                        onSelectTag(tag.tagId)
                    } label: {
                        TagItemView(tag: tag)
                    }
                    .buttonStyle(.plain)
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
