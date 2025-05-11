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
    
    let collections: [YabaCollection]
    let onNavigationCallback: (YabaCollection) -> Void
    
    private let labelTitle: String
    private let noCollectionsTitle: String
    private let noCollectionsMessage: String
    private let collectionIcon: String
    
    init(
        collectionType: CollectionType,
        collections: [YabaCollection],
        onNavigationCallback: @escaping (YabaCollection) -> Void
    ) {
        self.collections = collections
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
        ListSection(
            collections: collections,
            labelTitle: labelTitle,
            collectionIcon: collectionIcon,
            onNavigationCallback: onNavigationCallback
        )
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

private struct ListSection: View {
    @State
    private var isExpanded: Bool = true
    
    let collections: [YabaCollection]
    let labelTitle: String
    let collectionIcon: String
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
        Section(isExpanded: $isExpanded) {
            if collections.isEmpty {
            } else {
                ForEach(collections) { collection in
                    CollectionItemView(
                        collection: collection,
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
}

private struct GridSection: View {
    @State
    private var isExpanded: Bool = true
    
    let collections: [YabaCollection]
    let labelTitle: String
    let collectionIcon: String
    let onNavigationCallback: (YabaCollection) -> Void
    
    var body: some View {
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
                
            } else {
                ForEach(collections) { collection in
                    CollectionItemView(
                        collection: collection,
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
            Section {} header: { }
        } else {
            ForEach(collections) { collection in
                CollectionItemView(
                    collection: collection,
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
}

#Preview {
    HomeCollectionView(
        collectionType: .folder,
        collections: [],
        onNavigationCallback: { _ in }
    )
}

#Preview {
    HomeCollectionView(
        collectionType: .tag,
        collections: [],
        onNavigationCallback: { _ in }
    )
}
