//
//  MoveManager.swift
//  YABA
//
//  Created by Ali Taha on 12.10.2025.
//

import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
@preconcurrency
class MoveManager {
    private let modelContext: ModelContext = YabaModelContainer.getContext()
    
    func onMoveFolder(
        from folder1ID: String,
        to folder2ID: String
    ) {
        // MOVE CHILDREN TO SELF, SKIP
        if folder1ID == folder2ID { return }
        
        let folder1: YabaCollection? = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder1ID
                }
            )
        ).first
        
        guard let folder1 else { return }
        
        let folder2: YabaCollection? = try? modelContext.fetch(
            FetchDescriptor<YabaCollection>(
                predicate: #Predicate {
                    $0.collectionId == folder2ID
                }
            )
        ).first
        
        guard let folder2 else { return }
        
        // MOVE CHILDREN TO IT'S PARENT AGAIN, SKIP
        if folder2.children.contains(where: { $0.collectionId == folder1ID }) {
            return
        }
        
        // MOVE PARENT TO CHILDREN IS ILLEGAL, SKIP
        if folder1.getDescendants().contains(where: { $0.collectionId == folder2ID }) {
            return
        }
        
        do {
            if let parentOfFolder1 = folder1.parent {
                parentOfFolder1.children.removeAll(where: { $0.collectionId == folder1ID })
            }
            folder2.children.append(folder1)
            try modelContext.save()
        } catch {
            return
        }
    }
}

extension EnvironmentValues {
    @Entry var moveManager: MoveManager = .init()
}
