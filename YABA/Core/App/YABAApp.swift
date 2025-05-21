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
    private var toastManager: ToastManager = .init()
    
    @State
    private var deepLinkManager: DeepLinkManager = .init()
    
    @State
    private var appState: AppState = .init()
    
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
                .environment(\.appState, appState)
                .environment(\.deepLinkManager, deepLinkManager)
                .preferredColorScheme(preferredTheme.getScheme())
                .onAppear {
                    setupForMacCatalyst()
                }
            #if targetEnvironment(macCatalyst)
                .onOpenURL { url in
                    deepLinkManager.handleDeepLink(url)
                }
            #endif
        }
    }
    
    func setupForMacCatalyst() {
        #if targetEnvironment(macCatalyst)
        (UIApplication.shared.connectedScenes.first as? UIWindowScene)?.titlebar?.titleVisibility = .hidden
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.forEach { windowScene in
            windowScene.sizeRestrictions?.minimumSize = CGSize(width: 1600, height: 960)
        }
        
        StatusMenuHelper.setStatusMenuEnabled()
        #endif
    }
}
