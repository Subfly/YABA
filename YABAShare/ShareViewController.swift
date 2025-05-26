//
//  ShareViewController.swift
//  YABAShare
//
//  Created by Ali Taha on 21.04.2025.
//

import UIKit
import SwiftUI
import SwiftData
import UniformTypeIdentifiers
import OSLog

@objc(ShareViewController)
class ShareViewController: UIViewController {
    private var logger: Logger = .init()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        Task {
            await handleSharedItems()
        }
    }
    
    private func handleSharedItems() async {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            logger.log(level: .error, "[YABA_SHARE] No extension items found")
            close()
            return
        }
        
        var foundURLs: [URL] = []
        
        for item in extensionItems {
            guard let attachments = item.attachments else { continue }
            
            for provider in attachments {
                if let url = await extractURL(from: provider) {
                    logger.log("[YABA_SHARE] Found URL: \(url.absoluteString)")
                    foundURLs.append(url)
                }
            }
        }
        
        guard let firstURL = foundURLs.first else {
            logger.log(level: .error, "[YABA_SHARE] No valid URL found in any item")
            close()
            return
        }
        
        DispatchQueue.main.async {
            self.createContentView(link: firstURL.absoluteString)
        }
    }
    
    private func extractURL(from provider: NSItemProvider) async -> URL? {
        if provider.canLoadObject(ofClass: URL.self) {
            return try? await withCheckedThrowingContinuation { continuation in
                _ = provider.loadObject(ofClass: URL.self) { url, error in
                    if let url {
                        continuation.resume(returning: url)
                    } else {
                        continuation.resume(returning: nil)
                    }
                }
            }
        }
        
        if provider.hasItemConformingToTypeIdentifier("public.url") {
            if let item = try? await provider.loadItem(forTypeIdentifier: "public.url") as? URL {
                return item
            }
        }
        
        if provider.hasItemConformingToTypeIdentifier("public.plain-text") {
            if let text = try? await provider.loadItem(forTypeIdentifier: "public.plain-text") as? String,
               let url = URL(string: text),
               ["http", "https"].contains(url.scheme?.lowercased()) {
                return url
            }
        }
        
        return nil
    }
    
    private func createContentView(link: String) {
        let contentView = UIHostingController(
            rootView: BookmarkCreationContent(
                bookmarkToEdit: nil,
                collectionToFill: nil,
                link: link,
                onExitRequested: close
            ).modelContainer(for: [Bookmark.self, YabaCollection.self])
        )
        
        self.addChild(contentView)
        self.view.addSubview(contentView.view)
        contentView.view.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            contentView.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentView.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            contentView.view.leftAnchor.constraint(equalTo: view.leftAnchor),
            contentView.view.rightAnchor.constraint(equalTo: view.rightAnchor)
        ])
    }
    
    private func close() {
        self.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
