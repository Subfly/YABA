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
        ) {
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
}

private struct ListSection<NoCollectionView: View>: View {
    @State
    private var isExpandedGeneral: Bool = true
    
    @State
    private var isExpandedMobile: Bool = false
    
    let collections: [YabaCollection]
    let labelTitle: String
    let collectionIcon: String
    let onNavigationCallback: (YabaCollection) -> Void
    @ViewBuilder let noCollectionView: NoCollectionView
    
    var body: some View {
        if UIDevice.current.userInterfaceIdiom == .phone {
            mobileView
        } else {
            generalView
        }
    }
    
    @ViewBuilder
    private var mobileView: some View {
        generateHeaderView(includeExpansion: false)
        if collections.isEmpty {
            noCollectionView
        } else {
            if collections.count <= 10 {
                generateItems(
                    for: collections,
                    includeSpacerAtStart: true
                )
            } else {
                generateItems(
                    for: Array(collections.prefix(10)),
                    includeSpacerAtStart: true
                )
                if isExpandedMobile {
                    generateItems(
                        for: Array(collections.suffix(from: 10)),
                        includeSpacerAtStart: false
                    )
                }
            }
        }
        if collections.count > 10 {
            HStack {
                Spacer()
                Label {
                    Text(
                        isExpandedMobile
                        ? LocalizedStringKey("Show Less Label")
                        : LocalizedStringKey("Show More Label")
                    )
                    .font(.callout)
                    .foregroundStyle(Color(.secondaryLabel))
                } icon: {
                    YabaIconView(bundleKey: isExpandedMobile ? "arrow-up-01" : "arrow-down-01")
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                        .foregroundStyle(Color(.secondaryLabel))
                }
                .labelStyle(InverseLabelStyle())
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation {
                        isExpandedMobile.toggle()
                    }
                }
                Spacer()
            }.padding(.top)
        }
    }
    
    @ViewBuilder
    private var generalView: some View {
        generateHeaderView(includeExpansion: true)
        if isExpandedGeneral {
            if collections.isEmpty {
                noCollectionView
            } else {
                generateItems(
                    for: collections,
                    includeSpacerAtStart: true
                )
            }
        } else {
            EmptyView()
        }
    }
    
    @ViewBuilder
    private func generateHeaderView(includeExpansion: Bool = false) -> some View {
        HStack {
            Label {
                Text(LocalizedStringKey(labelTitle))
                    .font(.headline)
                    .fontWeight(.semibold)
            } icon: {
                YabaIconView(bundleKey: collectionIcon)
                    .scaledToFit()
                    .frame(width: 22, height: 22)
            }
            .foregroundStyle(.secondary)
            .font(.headline)
            Spacer()
            if includeExpansion {
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                    .foregroundStyle(.secondary)
                    .rotationEffect(isExpandedGeneral ? .init(degrees: 90) : .zero)
                    .animation(.smooth, value: isExpandedGeneral)
            }
        }
        .contentShape(Rectangle())
        .padding(.horizontal)
        .padding(.bottom)
        .onTapGesture {
            withAnimation {
                isExpandedGeneral.toggle()
            }
        }
    }
    
    @ViewBuilder
    private func generateItems(
        for given: [YabaCollection],
        includeSpacerAtStart: Bool,
    ) -> some View {
        ForEach(given) { collection in
            if includeSpacerAtStart && given.first?.collectionId == collection.collectionId {
                SeparatorItemView()
            }
            Spacer().frame(height: 1)
            CollectionItemView(
                collection: collection,
                isInHome: true,
                isInSelectionMode: false,
                isInBookmarkDetail: false,
                onDeleteCallback: { _ in },
                onEditCallback: { _ in },
                onNavigationCallback: onNavigationCallback
            )
            Spacer().frame(height: 1)
            SeparatorItemView()
            Spacer().frame(height: 1)
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
                        isInHome: true,
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
                    isInHome: true,
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
