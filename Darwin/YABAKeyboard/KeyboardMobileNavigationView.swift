//
//  KeyboardMobileNavigationView.swift
//  YABA
//
//  Created by Ali Taha on 21.08.2025.
//

import SwiftUI
import SwiftData

internal struct KeyboardMobileNavigationView: View {
    @Query(
        filter: #Predicate<YabaCollection> { collection in
            collection.parent == nil
        }
    )
    private var collections: [YabaCollection]
    
    @State
    private var path: [NavigationDestination] = []
    
    let onClickBookmark: (String) -> Void
    let onAccept: () -> Void
    let onDelete: () -> Void
    
    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                ScrollView {
                    LazyVStack {
                        Spacer().frame(height: 24)
                        HomeCollectionView(
                            collectionType: .folder,
                            collections: collections.filter { $0.collectionType == .folder },
                            onNavigationCallback: { collection in
                                path.append(.collectionDetail(collection: collection))
                            }
                        )
                        Spacer().frame(height: 24)
                        HomeCollectionView(
                            collectionType: .tag,
                            collections: collections.filter { $0.collectionType == .tag },
                            onNavigationCallback: { collection in
                                path.append(.collectionDetail(collection: collection))
                            }
                        )
                        Spacer().frame(height: 24)
                    }
                }
                .scrollContentBackground(.hidden)
            }
            .navigationTitle(Text("YABA"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onDelete()
                    } label: {
                        Label {
                            Text("Delete")
                        } icon: {
                            YabaIconView(bundleKey: "backward-01")
                        }
                    }.buttonRepeatBehavior(.enabled)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onAccept()
                    } label: {
                        Label {
                            Text("Done")
                        } icon: {
                            YabaIconView(bundleKey: "checkmark-circle-02")
                        }
                    }
                }
            }
            .navigationDestination(for: NavigationDestination.self) { destination in
                switch destination {
                case .collectionDetail(let collection): MobileCollectionDetail(
                    collection: collection,
                    onNavigationCallback: { bookmark in
                        onClickBookmark(bookmark.link)
                    },
                    onAcceptKeyboard: onAccept,
                    onDeleteKeyboard: onDelete
                ).navigationBarBackButtonHidden()
                default: EmptyView()
                }
            }
        }
    }
}
