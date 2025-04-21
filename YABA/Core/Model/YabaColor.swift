//
//  YabaColor.swift
//  YABA
//
//  Created by Ali Taha on 13.10.2024.
//
// swiftlint:disable all

import SwiftUI

enum YabaColor: Int, Codable, CaseIterable {
    case none
    case blue
    case brown
    case cyan
    case gray
    case green
    case indigo
    case mint
    case orange
    case pink
    case purple
    case red
    case teal
    case yellow

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
