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
    @AppStorage(Constants.preferredThemeKey)
    private var preferredTheme: ThemeType = .system
    
    @State
    var toastManager: ToastManager = .init()
    
    var body: some Scene {
        WindowGroup {
            YabaNavigationView()
                .modelContainer(
                    for: [Bookmark.self, YabaCollection.self],
                    inMemory: false,
                    isAutosaveEnabled: false,
                    isUndoEnabled: false
                )
                .environment(toastManager)
                .onAppear {
                    setupForMacCatalyst()
                }
                .preferredColorScheme(preferredTheme.getScheme())
        }
    }
    
    func setupForMacCatalyst() {
        #if targetEnvironment(macCatalyst)
        (UIApplication.shared.connectedScenes.first as? UIWindowScene)?.titlebar?.titleVisibility = .hidden
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.forEach { windowScene in
            windowScene.sizeRestrictions?.minimumSize = CGSize(width: 1600, height: 960)
        }
        #endif
    }
}
