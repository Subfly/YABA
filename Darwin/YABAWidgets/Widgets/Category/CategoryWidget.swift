//
//  CategoryWidget.swift
//  YABAKeyboard
//
//  Created by Ali Taha on 24.08.2025.
//

import SwiftUI
import WidgetKit

internal struct CategoryWidget: Widget {
    let kind: String = "YABA Category Widget"
    
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: CategoryAppIntent.self,
            provider: CategoryProvider()
        ) { entry in
            CategoryView(entry: entry)
                .containerBackground(.clear, for: .widget)
                .modelContext(YabaModelContainer.getContext())
        }
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}

private struct CategoryView: View {
    var entry: CategoryProvider.Entry
    
    var body: some View {
        ZStack {
            AnimatedGradient(
                collectionColor: entry.configuration.selectedCollection?.displayColor == nil
                ? .accentColor
                : entry.configuration.selectedCollection!.displayColor.getUIColor()
            ).clipShape(RoundedRectangle(cornerRadius: 16))
            
            if let collection = entry.configuration.selectedCollection {
                if let url = URL(string: "yaba://collection?id=\(collection.id)") {
                    Link(destination: url) {
                        CollectionView(collection: collection)
                    }
                }
            }
        }
    }
}

private struct CollectionView: View {
    let collection: CategoryCollectionEntity
    
    var body: some View {
        VStack {
            HStack {
                YabaIconView(bundleKey: collection.displayIconNameString)
                    .scaledToFit()
                    .frame(width: 32, height: 32)
                    .foregroundStyle(collection.displayColor.getUIColor())
                    .padding()
                    .background {
                        Circle()
                            .foregroundStyle(
                                collection.displayColor.getUIColor().opacity(0.2)
                            )
                    }
                Spacer()
                HStack {
                    Text("\(collection.bookmarkCount)")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                    YabaIconView(bundleKey: "bookmark-02")
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            HStack {
                if collection.displayString == Constants.uncategorizedCollectionLabelKey {
                    Text(LocalizedStringKey(Constants.uncategorizedCollectionLabelKey))
                        .lineLimit(2)
                        .font(.title2)
                        .fontWeight(.bold)
                } else {
                    Text(collection.displayString)
                        .lineLimit(2)
                        .font(.title2)
                        .fontWeight(.bold)
                }
                Spacer()
            }
        }.padding()
    }
}

#Preview(as: .systemSmall) {
    CategoryWidget()
} timeline: {
    CategoryEntry(date: .now, configuration: CategoryAppIntent())
}
