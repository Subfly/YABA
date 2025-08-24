//
//  AppIntent.swift
//  YABAWidgets
//
//  Created by Ali Taha on 22.08.2025.
//

import WidgetKit
import AppIntents
import SwiftData

// MARK: - Folder Entity for App Intent Configuration
internal struct FolderEntity: AppEntity {
    let id: String
    let displayString: String
    let displayIconNameString: String
    let displayColor: YabaColor
    
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Folder"
    static var defaultQuery = FolderEntityQuery()
    
    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(displayString)")
    }
}

internal struct FolderEntityQuery: EntityQuery {
    func entities(for identifiers: [FolderEntity.ID]) async throws -> [FolderEntity] {
        return try await suggestedEntities()
    }
    
    func suggestedEntities() async throws -> [FolderEntity] {
        var folders: [FolderEntity] = [
            FolderEntity(
                id: "recents",
                displayString: "Recents",
                displayIconNameString: "recents",
                displayColor: .none
            )
        ]
        
        do {
            let context = YabaModelContainer.getContext()
            let type = CollectionType.folder.rawValue
            let descriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate { $0.type == type },
                sortBy: [SortDescriptor(\YabaCollection.label)]
            )
            let collections = try context.fetch(descriptor)
            
            let folderEntities = collections.map { collection in
                FolderEntity(
                    id: collection.collectionId,
                    displayString: collection.label,
                    displayIconNameString: collection.icon,
                    displayColor: collection.color
                )
            }
            
            folders.append(contentsOf: folderEntities)
        } catch {
            print("Error fetching folders for widget configuration: \(error)")
        }
        
        return folders
    }
    
    func entities(matching string: String) async throws -> [FolderEntity] {
        let allFolders = try await suggestedEntities()
        return allFolders.filter { $0.displayString.localizedCaseInsensitiveContains(string) }
    }
}

// MARK: - App Intent Configuration
internal struct BookmarkListAppIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource { "Bookmark List Configuration Title" }
    static var description: IntentDescription { "Bookmark List Configuration Description" }
    
    @Parameter(title: "Folder Selection Title", description: "Choose a folder to show bookmarks from")
    var selectedFolder: FolderEntity?
    
    init() {
        self.selectedFolder = FolderEntity(
            id: "recents",
            displayString: "Recents",
            displayIconNameString: "recents",
            displayColor: .none
        )
    }
    
    init(selectedFolder: FolderEntity?) {
        self.selectedFolder = selectedFolder
    }
}
