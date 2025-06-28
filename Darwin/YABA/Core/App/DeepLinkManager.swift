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

@Observable
class DeepLinkManager {
    var saveRequest: DeepLinkSaveBookmarkRequest? = nil
    var openRequest: DeepLinkOpenBookmarkRequest? = nil
    
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
                openRequest = DeepLinkOpenBookmarkRequest(bookmarkId: id)
            }
        }
    }
    
    func onHandleDeeplink() {
        saveRequest = nil
        openRequest = nil
    }
}

extension EnvironmentValues {
    @Entry var deepLinkManager: DeepLinkManager = .init()
}
