//
//  AppIntent.swift
//  YABAWidgets
//
//  Created by Ali Taha on 22.08.2025.
//

import WidgetKit
import AppIntents
import SwiftData

// MARK: - Collection Entity for App Intent Configuration
internal struct CollectionEntity: AppEntity {
    let id: String
    let displayString: String
    let displayIconNameString: String
    let displayColor: YabaColor
    let collectionType: String // "folder" or "tag" or "recents"
    
    static var typeDisplayRepresentation: TypeDisplayRepresentation = TypeDisplayRepresentation(
        name: "Collection",
        numericFormat: "\(placeholder: .int) collections"
    )
    static var defaultQuery = CollectionEntityQuery()
    
    var displayRepresentation: DisplayRepresentation {
        let title: LocalizedStringResource = if displayString == Constants.uncategorizedCollectionLabelKey {
            LocalizedStringResource(stringLiteral: Constants.uncategorizedCollectionLabelKey)
        } else {
            "\(displayString)"
        }
        
        return DisplayRepresentation(
            title: title,
            subtitle: collectionType == "recents" ? "Recent Bookmarks" : (collectionType == "folder" ? "Folder" : "Tag"),
            image: .init(
                named: displayIconNameString,
                isTemplate: true,
                displayStyle: .default
            )
        )
    }
    
    static var caseDisplayRepresentation: TypeDisplayRepresentation {
        TypeDisplayRepresentation(name: "Collections")
    }
}

internal struct CollectionEntityQuery: EntityQuery {
    func entities(for identifiers: [CollectionEntity.ID]) async throws -> [CollectionEntity] {
        // If specific identifiers are requested, try to find them first
        let allCollections = try await suggestedEntities()
        if identifiers.isEmpty {
            return allCollections
        }
        return allCollections.filter { identifiers.contains($0.id) }
    }
    
    func suggestedEntities() async throws -> [CollectionEntity] {
        var collections: [CollectionEntity] = [
            CollectionEntity(
                id: "recents",
                displayString: "Recents",
                displayIconNameString: "clock-01",
                displayColor: .none,
                collectionType: "recents"
            )
        ]
        
        do {
            let context = YabaModelContainer.getContext()
            
            // Fetch folders
            let folderType = CollectionType.folder.rawValue
            let folderDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { $0.type == folderType },
                sortBy: [SortDescriptor(\YabaCollection.label)]
            )
            let folders = try context.fetch(folderDescriptor)
            
            let folderEntities = folders.map { collection in
                CollectionEntity(
                    id: collection.collectionId,
                    displayString: collection.label,
                    displayIconNameString: collection.icon,
                    displayColor: collection.color,
                    collectionType: "folder"
                )
            }
            
            // Fetch tags
            let tagType = CollectionType.tag.rawValue
            let tagDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { $0.type == tagType },
                sortBy: [SortDescriptor(\YabaCollection.label)]
            )
            let tags = try context.fetch(tagDescriptor)
            
            let tagEntities = tags.map { collection in
                CollectionEntity(
                    id: collection.collectionId,
                    displayString: collection.label,
                    displayIconNameString: collection.icon,
                    displayColor: collection.color,
                    collectionType: "tag"
                )
            }
            
            collections.append(contentsOf: folderEntities)
            collections.append(contentsOf: tagEntities)
        } catch {
            print("Error fetching collections for widget configuration: \(error)")
        }
        
        return collections
    }
    
    func entities(matching string: String) async throws -> [CollectionEntity] {
        let allCollections = try await suggestedEntities()
        return allCollections.filter { $0.displayString.localizedCaseInsensitiveContains(string) }
    }
    
    func defaultResult() async -> CollectionEntity? {
        do {
            let collections = try await suggestedEntities()
            return collections.first
        } catch {
            return CollectionEntity(
                id: "recents",
                displayString: "Recents",
                displayIconNameString: "clock-01",
                displayColor: .none,
                collectionType: "recents"
            )
        }
    }
}

// MARK: - Category Collection Entity (excludes recents)
internal struct CategoryCollectionEntity: AppEntity {
    let id: String
    let displayString: String
    let displayIconNameString: String
    let displayColor: YabaColor
    let collectionType: String // "folder" or "tag" (no "recents")
    let bookmarkCount: Int
    
    static var typeDisplayRepresentation: TypeDisplayRepresentation = TypeDisplayRepresentation(
        name: "Category Collection",
        numericFormat: "\(placeholder: .int) collections"
    )
    static var defaultQuery = CategoryCollectionEntityQuery()
    
    var displayRepresentation: DisplayRepresentation {
        let title: LocalizedStringResource = if displayString == Constants.uncategorizedCollectionLabelKey {
            LocalizedStringResource(stringLiteral: Constants.uncategorizedCollectionLabelKey)
        } else {
            "\(displayString)"
        }
        
        return DisplayRepresentation(
            title: title,
            subtitle: collectionType == "folder" ? "Folder" : "Tag",
            image: .init(
                named: displayIconNameString,
                isTemplate: true,
                displayStyle: .default
            )
        )
    }
    
    static var caseDisplayRepresentation: TypeDisplayRepresentation {
        TypeDisplayRepresentation(name: "Category Collections")
    }
}

// MARK: - Category-specific Collection Query (excludes recents)
internal struct CategoryCollectionEntityQuery: EntityQuery {
    func entities(for identifiers: [CategoryCollectionEntity.ID]) async throws -> [CategoryCollectionEntity] {
        // If specific identifiers are requested, try to find them first
        let allCollections = try await suggestedEntities()
        if identifiers.isEmpty {
            return allCollections
        }
        return allCollections.filter { identifiers.contains($0.id) }
    }
    
    func suggestedEntities() async throws -> [CategoryCollectionEntity] {
        var collections: [CategoryCollectionEntity] = []
        
        do {
            let context = YabaModelContainer.getContext()
            
            // Fetch folders
            let folderType = CollectionType.folder.rawValue
            let folderDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { $0.type == folderType },
                sortBy: [SortDescriptor(\YabaCollection.label)]
            )
            let folders = try context.fetch(folderDescriptor)
            
            let folderEntities = folders.map { collection in
                CategoryCollectionEntity(
                    id: collection.collectionId,
                    displayString: collection.label,
                    displayIconNameString: collection.icon,
                    displayColor: collection.color,
                    collectionType: "folder",
                    bookmarkCount: collection.bookmarks?.count ?? 0
                )
            }
            
            // Fetch tags
            let tagType = CollectionType.tag.rawValue
            let tagDescriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { $0.type == tagType },
                sortBy: [SortDescriptor(\YabaCollection.label)]
            )
            let tags = try context.fetch(tagDescriptor)
            
            let tagEntities = tags.map { collection in
                CategoryCollectionEntity(
                    id: collection.collectionId,
                    displayString: collection.label,
                    displayIconNameString: collection.icon,
                    displayColor: collection.color,
                    collectionType: "tag",
                    bookmarkCount: collection.bookmarks?.count ?? 0
                )
            }
            
            collections.append(contentsOf: folderEntities)
            collections.append(contentsOf: tagEntities)
        } catch {
            print("Error fetching collections for category widget configuration: \(error)")
        }
        
        return collections
    }
    
    func entities(matching string: String) async throws -> [CategoryCollectionEntity] {
        let allCollections = try await suggestedEntities()
        return allCollections.filter { $0.displayString.localizedCaseInsensitiveContains(string) }
    }
    
    func defaultResult() async -> CategoryCollectionEntity? {
        do {
            let collections = try await suggestedEntities()
            return collections.first
        } catch {
            // Return nil if no folders/tags are available - category widget shouldn't have a fallback to recents
            return nil
        }
    }
}

// MARK: - App Intent Configuration
internal struct BookmarkListAppIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Bookmark List Configuration Title" }
    static var description: IntentDescription { "Bookmark List Configuration Description" }
    
    @Parameter(
        title: "Collection Selection Title", 
        description: "Choose a folder or tag to show bookmarks from",
        requestValueDialog: "Choose Collection Dialog Title"
    )
    var selectedFolder: CollectionEntity?
    
    static var parameterSummary: some ParameterSummary {
        Summary("Show bookmarks from \(\.$selectedFolder)")
    }
    
    init() {
        // Default will be set dynamically to first available collection
        self.selectedFolder = nil
    }
    
    init(selectedFolder: CollectionEntity?) {
        self.selectedFolder = selectedFolder
    }
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}

internal struct CategoryAppIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Category Configuration Title" }
    static var description: IntentDescription { "Category Configuration Description" }
    
    @Parameter(
        title: "Collection Selection Title", 
        description: "Choose a folder or tag to display",
        requestValueDialog: "Choose Collection Dialog Title"
    )
    var selectedCollection: CategoryCollectionEntity?
    
    static var parameterSummary: some ParameterSummary {
        Summary("Display \(\.$selectedCollection)")
    }
    
    init() {
        // Default will be set dynamically to first available collection
        self.selectedCollection = nil
    }
    
    init(selectedCollection: CategoryCollectionEntity?) {
        self.selectedCollection = selectedCollection
    }
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}
