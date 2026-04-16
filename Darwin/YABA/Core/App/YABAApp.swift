//
//  YABAApp.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI
import SwiftData
import TipKit
import WidgetKit

@main
struct YABAApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var appDelegate
    
    @AppStorage(Constants.preferredThemeKey)
    private var preferredTheme: ThemePreference = .system
    
    @State
    private var deepLinkManager: DeepLinkManager = .init()
    
    // Sync disabled — preserve wiring when re-enabling NetworkSyncManager.
    // @State
    // private var networkSyncManager: NetworkSyncManager = .init()
    
    var body: some Scene {
        WindowGroup {
            YabaNavigationView()
                .modelContext(try! ParityModelContainer.makeContext())
                // .environment(\.deepLinkManager, deepLinkManager)
                // .environment(\.networkSyncManager, networkSyncManager)
                .preferredColorScheme(preferredTheme.getScheme())
                .onAppear {
                    setupForMacCatalyst()
                    try? Tips.configure()
                    WidgetCenter.shared.reloadAllTimelines()
                }
                .onOpenURL { url in
                    deepLinkManager.handleDeepLink(url)
                }
                .onReceive(NotificationCenter.default.publisher(for: .didReceiveDeepLink)) { notif in
                    if let url = notif.object as? URL {
                        deepLinkManager.handleDeepLink(url)
                    }
                }
        }
        #if targetEnvironment(macCatalyst)
        // Maybe one day I'll add some...
        .commands {
            CommandGroup(replacing: .newItem) { }
            CommandGroup(replacing: .undoRedo) { }
            CommandGroup(replacing: .pasteboard) { }
            CommandGroup(replacing: .importExport) { }
            CommandGroup(replacing: .newItem) { }
            CommandGroup(replacing: .pasteboard) { }
            CommandGroup(replacing: .printItem) { }
            CommandGroup(replacing: .saveItem) { }
            CommandGroup(replacing: .systemServices) { }
            CommandGroup(replacing: .textEditing) { }
        }
        .commands {
            CommandGroup(replacing: .textFormatting) { }
            CommandGroup(replacing: .toolbar) { }
            CommandGroup(replacing: .undoRedo) { }
        }
        #endif
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
