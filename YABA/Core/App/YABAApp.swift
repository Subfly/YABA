//
//  YABAApp.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI
import SwiftData

@main
struct YABAApp: App {
    var body: some Scene {
        WindowGroup {
            YabaNavigationView()
                .modelContainer(
                    for: [Bookmark.self, YabaCollection.self],
                    inMemory: false,
                    isAutosaveEnabled: false,
                    isUndoEnabled: false
                )
        }
    }
}
