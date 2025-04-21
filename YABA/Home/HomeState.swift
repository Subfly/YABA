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
    var isTagsExpanded: Bool = true
    var isFoldersExpanded: Bool = true
    var searchQuery: String = ""
    var isFABActive: Bool = false
    var shouldShowCreateContentSheet: Bool = false
    var selectedContentCreationType: CollectionType? = nil
    var shouldShowBackground: Bool = false
}
