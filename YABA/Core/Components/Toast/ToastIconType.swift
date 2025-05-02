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
            "alert-02"
        case .success:
            "checkmark-badge-02"
        case .error:
            "cancel-circle"
        default:
            "help-circle"
        }
    }
}
