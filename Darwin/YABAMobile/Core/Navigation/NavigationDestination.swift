//
//  NavigationDestination.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import Foundation

enum NavigationDestination: Hashable {
    case folderDetail(id: String)
    case tagDetail(id: String)
    case bookmarkDetail(bookmarkId: String)
    case search
}
