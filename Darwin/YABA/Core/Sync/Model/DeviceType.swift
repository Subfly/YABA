//
//  DeviceType.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation
import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// Device type for platform identification and UI symbols
enum DeviceType: String, Codable, CaseIterable {
    case computer = "computer"
    case tablet = "tablet"
    case phone = "phone"
    case unknown = "unknown"
    
    /// Automatically detect current device type
    static var current: DeviceType {
        #if targetEnvironment(macCatalyst)
        return .computer
        #elseif os(macOS)
        return .computer
        #elseif os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            return .tablet
        } else if UIDevice.current.userInterfaceIdiom == .phone {
            return .phone
        } else {
            return .unknown
        }
        #else
        return .unknown
        #endif
    }
    
    /// Display name for UI.
    var displayName: LocalizedStringKey {
        switch self {
        case .computer: return "Device Type Computer"
        case .tablet: return "Device Type Tablet"
        case .phone: return "Device Type Phone"
        case .unknown: return "Unknown"
        }
    }
    
    /// SF Symbol name for UI representation
    var symbolName: String {
        switch self {
        case .computer: return "computer"
        case .tablet: return "tablet-02"
        case .phone: return "smart-phone-02"
        case .unknown: return "help-square"
        }
    }
}
