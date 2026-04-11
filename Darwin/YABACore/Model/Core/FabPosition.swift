//
//  FabPosition.swift
//  YABACore
//
//  Parity with Compose `FabPosition`.
//

import Foundation
import SwiftUI

public enum FabPosition: String, Sendable, Codable, CaseIterable {
    case left
    case right
    case center

    public var uiIconName: String {
        switch self {
        case .left: return "circle-arrow-left-03"
        case .right: return "circle-arrow-right-03"
        case .center: return "circle-arrow-down-03"
        }
    }

    public func getUIIconName() -> String {
        uiIconName
    }

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .left: return "FAB Left Aligned"
        case .right: return "FAB Right Aligned"
        case .center: return "FAB Centered"
        }
    }
}
