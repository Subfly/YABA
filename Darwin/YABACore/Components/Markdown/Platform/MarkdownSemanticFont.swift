//
//  MarkdownSemanticFont.swift
//  YABACore
//
//  Shared semantic font tokens for selectable plain text (UIKit `UIFont.TextStyle` vs SwiftUI `Font`).
//

import SwiftUI

enum MarkdownSemanticFont: Equatable, Sendable {
    case largeTitle
    case title
    case title2
    case title3
    case headline
    case subheadline
    case body
    case callout
    case caption1
    case caption2

    var swiftUIFont: Font {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .body: .body
        case .callout: .callout
        case .caption1: .caption
        case .caption2: .caption2
        }
    }

    #if canImport(UIKit)
    var uiTextStyle: UIFont.TextStyle {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .body: .body
        case .callout: .callout
        case .caption1: .caption1
        case .caption2: .caption2
        }
    }

    func uiFont(weight: UIFont.Weight, monospaced: Bool) -> UIFont {
        let base = UIFont.preferredFont(forTextStyle: uiTextStyle)
        if monospaced {
            return UIFont.monospacedSystemFont(ofSize: base.pointSize, weight: weight)
        }
        return UIFont.systemFont(ofSize: base.pointSize, weight: weight)
    }
    #endif
}
