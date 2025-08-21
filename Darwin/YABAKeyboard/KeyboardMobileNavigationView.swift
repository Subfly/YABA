//
//  KeyboardMobileNavigationView.swift
//  YABA
//
//  Created by Ali Taha on 21.08.2025.
//

import SwiftUI
import SwiftData

internal struct KeyboardMobileNavigationView: View {
    @Query
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
                List {
                    HomeCollectionView(
                        collectionType: .folder,
                        collections: collections.filter { $0.collectionType == .folder },
                        onNavigationCallback: { collection in
                            path.append(.collectionDetail(collection: collection))
                        }
                    )
                    HomeCollectionView(
                        collectionType: .tag,
                        collections: collections.filter { $0.collectionType == .tag },
                        onNavigationCallback: { collection in
                            path.append(.collectionDetail(collection: collection))
                        }
                    )
                }
                .listStyle(.sidebar)
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
