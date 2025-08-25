//
//  DeepLinkManager.swift
//  YABA
//
//  Created by Ali Taha on 20.05.2025.
//

import Foundation
import SwiftUI

struct DeepLinkSaveBookmarkRequest: Identifiable, Equatable {
    let id: UUID = .init()
    let link: String
}

struct DeepLinkOpenBookmarkRequest: Identifiable, Equatable {
    let id: UUID = .init()
    let bookmarkId: String
}

struct DeepLinkOpenCollectionRequest: Identifiable, Equatable {
    let id: UUID = .init()
    let collectionId: String
}

@Observable
class DeepLinkManager {
    var saveRequest: DeepLinkSaveBookmarkRequest? = nil
    var openBookmarkRequest: DeepLinkOpenBookmarkRequest? = nil
    var openCollectionRequest: DeepLinkOpenCollectionRequest? = nil
    
    func handleDeepLink(_ url: URL) {
        if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
           components.scheme == "yaba" {
            if components.host == "save",
               let linkQueryItem = components.queryItems?.first(where: { $0.name == "link" }),
               let link = linkQueryItem.value {
                saveRequest = DeepLinkSaveBookmarkRequest(link: link)
            } else if components.host == "open",
                let idQueryItem = components.queryItems?.first(where: { $0.name == "id" }),
                let id = idQueryItem.value {
                openBookmarkRequest = DeepLinkOpenBookmarkRequest(bookmarkId: id)
            } else if components.host == "collection",
                let idQueryItem = components.queryItems?.first(where: { $0.name == "id" }),
                let id = idQueryItem.value {
                openCollectionRequest = DeepLinkOpenCollectionRequest(collectionId: id)
            }
        }
    }
    
    func onHandleDeeplink() {
        saveRequest = nil
        openBookmarkRequest = nil
        openCollectionRequest = nil
    }
}

extension EnvironmentValues {
    @Entry var deepLinkManager: DeepLinkManager = .init()
}
