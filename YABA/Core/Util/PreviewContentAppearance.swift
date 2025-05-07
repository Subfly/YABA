//
//  PreviewContentAppearance.swift
//  YABA
//
//  Created by Ali Taha on 7.05.2025.
//

import SwiftUI

enum PreviewContentAppearance: Int, Hashable, CaseIterable {
    case list, cardSmallImage, cardBigImage //, grid
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .list: ViewType.list.getUITitle()
        case .cardSmallImage: CardViewTypeImageSizing.small.getUITitle()
        case .cardBigImage: CardViewTypeImageSizing.big.getUITitle()
        //case .grid: LocalizedStringKey("View Grid")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .list: ViewType.list.getUIIconName()
        case .cardSmallImage: CardViewTypeImageSizing.small.getUIIconName()
        case .cardBigImage: CardViewTypeImageSizing.big.getUIIconName()
        //case .grid: LocalizedStringKey("View Grid")
        }
    }
}
