//
//  FloatingPanel.swift
//  YABA
//
//  Created by Ali Taha on 21.05.2025.
//

import AppKit

internal class FloatingPanel: NSPanel {
    init(contentViewController: NSViewController) {
        super.init(
            contentRect: NSRect(x: 0, y: 0, width: 350, height: 150),
            styleMask: [.titled, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        
        self.contentViewController = contentViewController
        self.isFloatingPanel = true
        self.level = .floating
        self.hidesOnDeactivate = false
        self.isReleasedWhenClosed = false
        
        self.backgroundColor = .clear
        self.isOpaque = false
        
        self.titleVisibility = .hidden
        self.titlebarAppearsTransparent = true
        
        self.standardWindowButton(.closeButton)?.isHidden = true
        self.standardWindowButton(.miniaturizeButton)?.isHidden = true
        self.standardWindowButton(.zoomButton)?.isHidden = true
        
        if let screen = NSScreen.main {
            let screenRect = screen.visibleFrame
            let x = screenRect.origin.x + (screenRect.width - 350) / 2
            let y = screenRect.origin.y + (screenRect.height - 150) / 2
            self.setFrameOrigin(NSPoint(x: x, y: y))
        }
    }
}
