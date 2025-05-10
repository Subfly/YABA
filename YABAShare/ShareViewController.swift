//
//  ShareViewController.swift
//  YABAShare
//
//  Created by Ali Taha on 21.04.2025.
//

import UIKit
import SwiftUI
import SwiftData
import OSLog

@objc(ShareViewController)
class ShareViewController: UIViewController {
    private var logger: Logger = .init()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard
            let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
            let itemProvider = extensionItem.attachments?.first else {
            self.logger.log(level: .error, "[YABA_SHARE] Unable to parse attechments")
            self.close()
            return
        }
        
        if itemProvider.hasItemConformingToTypeIdentifier("public.url") {
            itemProvider.loadItem(
                forTypeIdentifier: "public.url",
                options: nil
            ) { providedUrl, error in
                if error != nil {
                    self.logger.log(level: .error, "[YABA_SHARE] Unable to get URL")
                    self.close()
                    return
                }
                
                if let url = providedUrl as? NSURL {
                    if let link = url.absoluteString {
                        DispatchQueue.main.async {
                            self.createContentView(link: link)
                        }
                    } else {
                        self.logger.log(level: .error, "[YABA_SHARE] Unable to convert URL to Link")
                        self.close()
                        return
                    }
                } else {
                    self.logger.log(level: .error, "[YABA_SHARE] Unable to parse URL")
                    self.close()
                    return
                }
            }
        } else {
            self.close()
        }
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
        contentView.view.topAnchor.constraint(
            equalTo: self.view.topAnchor
        ).isActive = true
        contentView.view.bottomAnchor.constraint(
            equalTo: self.view.bottomAnchor
        ).isActive = true
        contentView.view.leftAnchor.constraint(
            equalTo: self.view.leftAnchor
        ).isActive = true
        contentView.view.rightAnchor.constraint(
            equalTo: self.view.rightAnchor
        ).isActive = true
    }
    
    private func close() {
        self.extensionContext?.completeRequest(
            returningItems: [],
            completionHandler: nil
        )
    }
}
