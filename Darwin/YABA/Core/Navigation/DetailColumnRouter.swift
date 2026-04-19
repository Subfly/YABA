//
//  DetailColumnRouter.swift
//  YABA
//
//  Drives the split-view detail column `NavigationStack` (search, folder, tag).
//

import SwiftUI

enum DetailDestination: Hashable, Sendable {
    case search
    case folder(String)
    case tag(String)
}

@MainActor
@Observable
final class DetailColumnRouter {
    var navigationPath = NavigationPath()

    func openSearch() {
        navigationPath.append(DetailDestination.search)
    }

    func openFolder(id: String) {
        navigationPath.append(DetailDestination.folder(id))
    }

    func openTag(id: String) {
        navigationPath.append(DetailDestination.tag(id))
    }
}
