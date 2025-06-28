//
//  ThemeType.swift
//  YABA
//
//  Created by Ali Taha on 3.05.2025.
//

import Foundation
import SwiftUI

enum ThemeType: Int, Hashable, CaseIterable {
    case light, dark, system
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .light: LocalizedStringKey("Theme Light")
        case .dark: LocalizedStringKey("Theme Dark")
        case .system: LocalizedStringKey("Theme System")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .light: "sun-03"
        case .dark: "moon-02"
        case .system: "smart-phone-02"
        }
    }
    
    func getScheme() -> ColorScheme? {
        return switch self {
        case .light: .light
        case .dark: .dark
        case .system: nil
        }
    }
}
