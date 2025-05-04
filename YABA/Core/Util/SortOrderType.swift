//
//  SortOrderType.swift
//  YABA
//
//  Created by Ali Taha on 4.05.2025.
//

import Foundation
import SwiftUI

enum SortOrderType: Int, Hashable, CaseIterable {
    case ascending, descending
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .ascending: LocalizedStringKey("Sort Order Ascending")
        case .descending: LocalizedStringKey("Sort Order Descending")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .ascending: "sorting-1-9"
        case .descending: "sorting-9-1"
        }
    }
}
