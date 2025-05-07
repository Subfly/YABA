//
//  ViewType.swift
//  YABA
//
//  Created by Ali Taha on 3.05.2025.
//

import Foundation
import SwiftUI

//TODO: OPEN UP GRID VIEW WHEN APPLE FINALLY DECIDES TO BRING RECYCLING TO LAZYVGRIDS
//TODO: Plan is to having the "list" as expandible to "card" and only having "list-grid"
enum ViewType: Int, Hashable, CaseIterable {
    case list, card //, grid
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .list: LocalizedStringKey("View List")
        case .card: LocalizedStringKey("View Card")
        //case .grid: LocalizedStringKey("View Grid")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .list: "list-view"
        case .card: "rectangular"
        //case .grid: "grid-view"
        }
    }
}
