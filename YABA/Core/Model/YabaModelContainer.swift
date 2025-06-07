//
//  YabaModelContainer.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation
import SwiftData

class YabaModelContainer {
    static func getContext() -> ModelContext {
        let container = configureAndGetContainer()
        let context = ModelContext(container)
        context.autosaveEnabled = false
        return context
    }
    
    private static func configureAndGetContainer() -> ModelContainer {
        let schema = Schema(YabaSchemaV1.models)
        
        let modelConfiguration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: false,
        )
        
        do {
            let container = try ModelContainer(
                for: schema,
                // One day, migrations may come here...
                configurations: [modelConfiguration],
            )
            
            return container
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }
}
