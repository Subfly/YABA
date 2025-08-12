//
//  YABAApp.swift
//  YABA
//
//  Created by Ali Taha on 18.04.2025.
//

import SwiftUI
import SwiftData
import TipKit

@main
struct YABAApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var appDelegate
    
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
                .modelContext(YabaModelContainer.getContext())
                .environment(toastManager)
                .environment(\.appState, appState)
                .environment(\.deepLinkManager, deepLinkManager)
                .preferredColorScheme(preferredTheme.getScheme())
                .onAppear {
                    setupForMacCatalyst()
                    try? Tips.configure()
                }
            #if targetEnvironment(macCatalyst)
                .onOpenURL { url in
                    deepLinkManager.handleDeepLink(url)
                }
            #endif
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
