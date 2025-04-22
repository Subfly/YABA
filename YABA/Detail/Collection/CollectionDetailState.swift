//
//  CollectionDetailState.swift
//  YABA
//
//  Created by Ali Taha on 20.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
internal class CollectionDetailState {
    var searchQuery: String = ""
    var shouldShowCreateBookmarkSheet: Bool = false
}
