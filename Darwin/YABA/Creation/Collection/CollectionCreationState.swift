//
//  CreateTagState.swift
//  YABA
//
//  Created by Ali Taha on 19.04.2025.
//

import Foundation
import SwiftUI

@MainActor
@Observable
internal class CollectionCreationState {
    var collectionName: String = ""
    var selectedIconName: String = ""
    var shouldShowIconPicker: Bool = false
    var shouldShowColorPicker: Bool = false
    var selectedColor: YabaColor = .none
    var selectedParent: YabaCollection? = nil
}
