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

@Observable
class DeepLinkManager {
    var request: DeepLinkSaveBookmarkRequest? = nil
    
    func handleDeepLink(_ url: URL) {
        if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
           components.scheme == "yaba",
           components.host == "save",
           let linkQueryItem = components.queryItems?.first(where: { $0.name == "link" }),
           let link = linkQueryItem.value {
            request = DeepLinkSaveBookmarkRequest(link: link)
        }
    }
    
    func onHandleDeeplink() {
        request = nil
    }
}

extension EnvironmentValues {
    @Entry var deepLinkManager: DeepLinkManager = .init()
}
