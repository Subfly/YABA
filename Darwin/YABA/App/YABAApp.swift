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
    }
}
