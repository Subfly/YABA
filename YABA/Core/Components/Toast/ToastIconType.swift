//
//  ToastIconType.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//


import Foundation

enum ToastIconType {
    case warning, success, hint, error, none
    
    func getIcon() -> String {
        switch self {
        case .warning:
            "exclamationmark.triangle"
        case .success:
            "party.popper"
        case .error:
            "x.circle"
        default:
            "questionmark.circle"
        }
    }
}
