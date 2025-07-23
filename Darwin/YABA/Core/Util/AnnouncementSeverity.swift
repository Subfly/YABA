//
//  AnnouncementSeverity.swift
//  YABA
//
//  Created by Ali Taha on 23.07.2025.
//

import SwiftUI

enum AnnouncementSeverity {
    case update, warning, urgent, typical
    
    func getUIColor() -> Color {
        switch self {
        case .update:
            return .blue
        case .warning:
            return .orange
        case .urgent:
            return .red
        case .typical:
            return .green
        }
    }
    
    func getUIIcon() -> String {
        return switch self {
        case .update: "party"
        case .warning: "alert-02"
        case .urgent: "spam"
        case .typical: "property-new"
        }
    }
}
