//
//  ViewType.swift
//  YABA
//
//  Created by Ali Taha on 3.05.2025.
//

import Foundation
import SwiftUI

enum ViewType: Int, Hashable, CaseIterable {
    case list, grid
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .list: LocalizedStringKey("View List")
        case .grid: LocalizedStringKey("View Grid")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .list: "list-view"
        case .grid: "grid-view"
        }
    }
}
