//
//  ShareViewController.swift
//  YABAShareMac
//
//  Created by Ali Taha on 18.05.2025.
//

import AppKit
import SwiftUI
import OSLog
import SwiftData

@objc(ShareViewController)
class ShareViewController: NSViewController {
    private var logger: Logger = .init()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.extractContentAndPresent()
    }
    
    private func extractContentAndPresent() {
        guard
            let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
            let attachments = extensionItem.attachments else {
            logger.error("[YABA_SHARE_MAC] No attachments found")
            close()
            return
        }
        
        for itemProvider in attachments {
            // Try loading as URL first
            if itemProvider.canLoadObject(ofClass: NSURL.self) {
                itemProvider.loadObject(ofClass: NSURL.self) { item, error in
                    if let url = item as? NSURL {
                        DispatchQueue.main.async {
                            self.presentSwiftUIView(link: url.absoluteString ?? "")
                        }
                    } else {
                        self.logger.error("[YABA_SHARE_MAC] Failed to cast loaded NSURL")
                        self.close()
                    }
                }
                return
            }
            
            // Fallback: Try loading as plain text
            if itemProvider.canLoadObject(ofClass: NSString.self) {
                itemProvider.loadObject(ofClass: NSString.self) { item, error in
                    if let link = item as? NSString {
                        DispatchQueue.main.async {
                            self.presentSwiftUIView(link: link as String)
                        }
                    } else {
                        self.logger.error("[YABA_SHARE_MAC] Failed to load plain-text URL")
                        self.close()
                    }
                }
                return
            }
        }
        
        // If no provider matched
        logger.error("[YABA_SHARE_MAC] No usable item provider found")
        close()
        
    }
    
    private func presentSwiftUIView(link: String) {
        let contentView = SimpleBookmarkCreationView(
            link: link,
            needsTopPadding: true,
            onClickClose: close
        ).modelContainer(for: [Bookmark.self, YabaCollection.self])
        
        let hostingController = NSHostingController(rootView: contentView)
        addChild(hostingController)
        
        self.view = hostingController.view
        self.view.frame = NSRect(x: 0, y: 0, width: 337.5, height: 225)
    }
    
    private func close() {
        self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
