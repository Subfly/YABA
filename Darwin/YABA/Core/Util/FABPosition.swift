//
//  FABPosition.swift
//  YABA
//
//  Created by Ali Taha on 11.09.2025.
//

import Foundation
import SwiftUI

enum FABPosition: Int, Hashable, CaseIterable {
    case left, right, center
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .left: "FAB Left Aligned"
        case .right: "FAB Right Aligned"
        case .center: "FAB Centered"
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .left: "circle-arrow-left-03"
        case .right: "circle-arrow-right-03"
        case .center: "circle-arrow-down-03"
        }
    }
}
