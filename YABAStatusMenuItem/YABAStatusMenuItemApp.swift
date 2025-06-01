//
//  YABAStatusMenuItemApp.swift
//  YABAStatusMenuItem
//
//  Created by Ali Taha on 17.05.2025.
//

import AppKit
import SwiftUI
import Carbon.HIToolbox
import OSLog

private let CREATE_BOOKMARK_NOTIFICATION_NAME = "showBookmarkPanel"

internal extension String {
    var fourCharCodeValue: FourCharCode {
        var result: FourCharCode = 0
        for char in utf16 {
            result = (result << 8) + FourCharCode(char)
        }
        return result
    }
}

internal extension Notification.Name {
    static let showBookmarkPanel = Notification.Name(CREATE_BOOKMARK_NOTIFICATION_NAME)
}

@main
struct YABAStatusMenuItemApp: App {
    @Environment(\.openWindow)
    private var openWindow
    
    @Environment(\.dismissWindow)
    private var dismiss
    
    @State
    private var panel: NSPanel?
    
    @State
    private var hotKeyRef: EventHotKeyRef?
    
    private let logger = Logger()
    
    init() {
        registerGlobalHotKey()
    }
    
    var body: some Scene {
        MenuBarExtra {
            Button {
                NSApplication.shared.activate(ignoringOtherApps: true)
                showPanel()
            } label: {
                Text("New Bookmark")
            }
            .keyboardShortcut("y", modifiers: [.shift, .command])
            Divider()
            Button {
                exit(0)
            } label: {
                Text("Quit")
            }.keyboardShortcut("q")
        } label: {
            Image(nsImage: getLabelIcon())
                .onReceive(NotificationCenter.default.publisher(for: .showBookmarkPanel)) { _ in
                    showPanel()
                }
        }
    }
    
    private func getLabelIcon() -> NSImage {
        let image: NSImage = NSImage(resource: .bookmark02)
        
        let ratio = image.size.height / image.size.width
        image.size.height = 18
        image.size.width = 18 / ratio
        
        image.isTemplate = true
        
        return image
    }
    
    private func showPanel() {
        if panel == nil {
            let hostingController = NSHostingController(
                rootView: SimpleBookmarkCreationView(
                    link: "",
                    needsTopPadding: true,
                    onClickClose: {
                        closePanelWithAnimation()
                    }
                )
                .background {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.ultraThinMaterial)
                }
                .modelContainer(
                    for: [YabaBookmark.self, YabaCollection.self, YabaDataLog.self],
                    inMemory: false,
                    isAutosaveEnabled: false,
                    isUndoEnabled: false
                )
            )
            
            panel = FloatingPanel(contentViewController: hostingController)
            panel?.alphaValue = 0
            panel?.makeKeyAndOrderFront(nil)
            NSAnimationContext.runAnimationGroup { context in
                context.duration = 0.2
                panel?.animator().alphaValue = 1
            }
        } else {
            panel?.alphaValue = 0
            panel?.makeKeyAndOrderFront(nil)
            NSAnimationContext.runAnimationGroup { context in
                context.duration = 0.2
                panel?.animator().alphaValue = 1
            }
        }
    }
    
    private func closePanelWithAnimation() {
        NSAnimationContext.runAnimationGroup({ context in
            context.duration = 0.2
            panel?.animator().alphaValue = 0
        }, completionHandler: {
            panel?.orderOut(nil)
            panel = nil
        })
    }
    
    private func registerGlobalHotKey() {
        let eventHotKeyID = EventHotKeyID(
            signature: OSType(UInt32("YABA".fourCharCodeValue)),
            id: 1
        )
        let modifierFlags: UInt32 = UInt32((cmdKey | shiftKey))  // CMD + SHIFT
        let keyCode: UInt32 = 16 // 'Y' key (key code from macOS key map)
        
        let status = RegisterEventHotKey(
            keyCode,
            modifierFlags,
            eventHotKeyID,
            GetEventDispatcherTarget(),
            0,
            &hotKeyRef
        )
        
        guard status == noErr else {
            logger.log(level: .error, "[YABA_SHARE] Failed to register hotkey")
            return
        }
        
        InstallEventHandler(
            GetEventDispatcherTarget(),
            { _, eventRef, _ in
                var hotKeyID = EventHotKeyID()
                GetEventParameter(
                    eventRef,
                    EventParamName(kEventParamDirectObject),
                    EventParamType(typeEventHotKeyID),
                    nil,
                    MemoryLayout.size(ofValue: hotKeyID),
                    nil,
                    &hotKeyID
                )
                
                if hotKeyID.signature == OSType(UInt32("YABA".fourCharCodeValue)) {
                    DispatchQueue.main.async {
                        NotificationCenter.default.post(name: .showBookmarkPanel, object: nil)
                    }
                }
                return noErr
            },
            1,
            [EventTypeSpec(
                eventClass: OSType(kEventClassKeyboard),
                eventKind: UInt32(kEventHotKeyPressed))],
            nil,
            nil
        )
    }
}
