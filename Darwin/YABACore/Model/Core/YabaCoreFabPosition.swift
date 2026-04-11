//
//  YabaCoreFabPosition.swift
//  YABACore
//
//  Parity with Compose `FabPosition`.
//

import Foundation

public enum YabaCoreFabPosition: String, Sendable, Codable, CaseIterable {
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
}
