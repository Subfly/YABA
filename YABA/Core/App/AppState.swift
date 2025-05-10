//
//  AppState.swift
//  YABA
//
//  Created by Ali Taha on 9.05.2025.
//

import Foundation
import SwiftUI

@Observable
class AppState {
    var selectedCollection: YabaCollection?
    var selectedBookmark: Bookmark?
}

extension EnvironmentValues {
    @Entry var appState: AppState = .init()
}
