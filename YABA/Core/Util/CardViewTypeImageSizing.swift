//
//  CardViewTypeImageSizing.swift
//  YABA
//
//  Created by Ali Taha on 7.05.2025.
//

import Foundation
import SwiftUI

enum CardViewTypeImageSizing: Int, Hashable, CaseIterable {
    case big, small
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .big: LocalizedStringKey("Card Image Sizing Big")
        case .small: LocalizedStringKey("Card Image Sizing Small")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .big: "image-composition-oval"
        case .small: "image-composition"
        }
    }
}
