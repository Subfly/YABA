//
//  YabaColor.swift
//  YABA
//
//  Created by Ali Taha on 13.10.2024.
//
// swiftlint:disable all

import SwiftUI

enum YabaColor: Int, Codable, CaseIterable {
    case none = 0
    case blue = 1
    case brown = 2
    case cyan = 3
    case gray = 4
    case green = 5
    case indigo = 6
    case mint = 7
    case orange = 8
    case pink = 9
    case purple = 10
    case red = 11
    case teal = 12
    case yellow = 13

    func getUIColor() -> Color {
        switch self {
        case .blue:
                .blue
        case .brown:
                .brown
        case .cyan:
                .cyan
        case .gray:
                .gray
        case .green:
                .green
        case .orange:
                .orange
        case .purple:
                .purple
        case .red:
                .red
        case .yellow:
                .yellow
        case .indigo:
                .indigo
        case .mint:
                .mint
        case .teal:
                .teal
        case .pink:
                .pink
        default:
                .accentColor
        }
    }
    
    func getUIText() -> String {
        switch self {
        case .blue:
                "Blue"
        case .brown:
                "Brown"
        case .cyan:
                "Cyan"
        case .gray:
                "Gray"
        case .green:
                "Green"
        case .orange:
                "Orange"
        case .purple:
                "Purple"
        case .red:
                "Red"
        case .yellow:
                "Yellow"
        case .indigo:
                "Indigo"
        case .mint:
                "Mint"
        case .teal:
                "Teal"
        case .pink:
                "Pink"
        default:
                "Theme Color"
        }
    }
}
