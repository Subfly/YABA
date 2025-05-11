//
//  NavigationDestination.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import Foundation

enum NavigationDestination: Hashable {
    case collectionDetail(collection: YabaCollection?)
    case bookmarkDetail(bookmark: Bookmark?)
    case settings
    case search
}
