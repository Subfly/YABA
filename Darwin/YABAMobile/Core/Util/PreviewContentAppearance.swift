//
//  PreviewContentAppearance.swift
//  YABA
//
//  Created by Ali Taha on 7.05.2025.
//

import SwiftUI

enum PreviewContentAppearance: Int, Hashable, CaseIterable {
    case list, cardSmallImage, cardBigImage, grid
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .list: ContentAppearance.list.getUITitle()
        case .cardSmallImage: CardImageSizing.small.getUITitle()
        case .cardBigImage: CardImageSizing.big.getUITitle()
        case .grid: ContentAppearance.grid.getUITitle()
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .list: ContentAppearance.list.getUIIconName()
        case .cardSmallImage: CardImageSizing.small.getUIIconName()
        case .cardBigImage: CardImageSizing.big.getUIIconName()
        case .grid: ContentAppearance.grid.getUIIconName()
        }
    }
}
