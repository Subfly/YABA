//
//  SortingPicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

enum SortingPickerType {
    case collection, bookmark
}

struct SortingPicker: View {
    @Environment(\.moveManager)
    private var moveManager
    
    @AppStorage(Constants.preferredCollectionSortingKey)
    private var preferredCollectionSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredBookmarkSortingKey)
    private var preferredBookmarkSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    let contentType: SortingPickerType
    
    var body: some View {
        /**
         * Bookmarks don't have custom sort feature
         * as it is impossible to hold sort order of bookmark
         * for each tag and folder. I know it is not really impossible
         * but YABA's database is not designed for that and it
         * will complicate the whole business logic a lot.
         */
        let availableCases = SortType.allCases.filter { type in
            if contentType == .bookmark {
                type != .custom
            } else {
                true
            }
        }
        
        let disableOrdering = if contentType == .collection {
            preferredCollectionSorting == .custom
        } else {
            false
        }
        
        Menu {
            generateSortTypes(with: availableCases)
            if !disableOrdering {
                orderTypeSection
            }
        } label: {
            generateLabel(orderingDisabled: disableOrdering)
        }
        .buttonStyle(.plain)
        .onChange(of: preferredCollectionSorting) { _, newValue in
            if contentType == .bookmark {
                return
            }
            
            if newValue == .custom {
                moveManager.onCustomSortCollections()
            }
        }
    }
    
    @ViewBuilder
    private func generateLabel(orderingDisabled: Bool) -> some View {
        HStack {
            Label {
                if contentType == .collection {
                    Text("Settings Collection Sorting Title")
                } else {
                    Text("Settings Bookmark Sorting Title")
                }
            } icon: {
                YabaIconView(bundleKey: "sorting-04")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
            }
            // Only visible in settings
            Spacer()
            HStack {
                if contentType == .collection {
                    YabaIconView(bundleKey: preferredCollectionSorting.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredCollectionSorting.getUITitle())
                } else {
                    YabaIconView(bundleKey: preferredBookmarkSorting.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredBookmarkSorting.getUITitle())
                }
                if !orderingDisabled {
                    YabaIconView(bundleKey: preferredSortOrder.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }
        }.contentShape(Rectangle())
    }
    
    @ViewBuilder
    private func generateSortTypes(with cases: [SortType]) -> some View {
        ForEach(cases, id: \.self) { type in
            Button {
                withAnimation {
                    if contentType == .collection {
                        preferredCollectionSorting = type
                    } else {
                        preferredBookmarkSorting = type
                    }
                }
            } label: {
                Label {
                    Text(type.getUITitle())
                } icon: {
                    YabaIconView(bundleKey: type.getUIIconName())
                }
            }
        }
    }
    
    @ViewBuilder
    private var orderTypeSection: some View {
        Divider()
        ForEach(SortOrderType.allCases, id: \.self) { type in
            Button {
                withAnimation {
                    preferredSortOrder = type
                }
            } label: {
                Label {
                    Text(type.getUITitle())
                } icon: {
                    YabaIconView(bundleKey: type.getUIIconName())
                }
            }
        }
    }
}

#Preview {
    SortingPicker(contentType: .collection)
}
