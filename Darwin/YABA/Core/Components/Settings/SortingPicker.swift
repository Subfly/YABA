//
//  SortingPicker.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import SwiftUI

struct SortingPicker: View {
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
    @AppStorage(Constants.preferredSortOrderKey)
    private var preferredSortOrder: SortOrderType = .ascending
    
    var body: some View {
        Menu {
            ForEach(SortType.allCases, id: \.self) { type in
                Button {
                    withAnimation {
                        preferredSorting = type
                    }
                } label: {
                    Label {
                        HStack {
                            if type == preferredSorting {
                                YabaIconView(bundleKey: "tick-01")
                            }
                            Text(type.getUITitle())
                        }
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            }
            Divider()
            ForEach(SortOrderType.allCases, id: \.self) { type in
                Button {
                    withAnimation {
                        preferredSortOrder = type
                    }
                } label: {
                    Label {
                        HStack {
                            if type == preferredSortOrder {
                                YabaIconView(bundleKey: "tick-01")
                            }
                            Text(type.getUITitle())
                        }
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            }
        } label: {
            HStack {
                Label {
                    Text("Settings Sorting Title")
                } icon: {
                    YabaIconView(bundleKey: "sorting-04")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                HStack {
                    YabaIconView(bundleKey: preferredSorting.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                    Text(preferredSorting.getUITitle())
                    YabaIconView(bundleKey: preferredSortOrder.getUIIconName())
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }.contentShape(Rectangle())
        }.buttonStyle(.plain)
    }
}

#Preview {
    SortingPicker()
}
