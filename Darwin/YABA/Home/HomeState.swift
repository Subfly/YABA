//
//  HomeState.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import SwiftUI

@Observable
@MainActor
internal class HomeState {
    var isFABActive: Bool = false
    var shouldShowCreateContentSheet: Bool = false
    var shouldShowCreateBookmarkSheet: Bool = false
    var shouldShowSyncSheet: Bool = false
    var selectedContentCreationType: CollectionType? = nil
    var shouldShowBackground: Bool = false
    var saveBookmarkRequest: DeepLinkSaveBookmarkRequest? = nil
}
