//
//  SortType.swift
//  YABA
//
//  Created by Ali Taha on 3.05.2025.
//

import Foundation
import SwiftUI

enum SortType: Int, Hashable, CaseIterable {
    case createdAt, editedAt, label, custom
    
    func getUITitle() -> LocalizedStringKey {
        return switch self {
        case .createdAt: LocalizedStringKey("Sort Created At")
        case .editedAt: LocalizedStringKey("Sort Edited At")
        case .label: LocalizedStringKey("Sort Label")
        case .custom: LocalizedStringKey("Sort Custom")
        }
    }
    
    func getUIIconName() -> String {
        return switch self {
        case .createdAt: "clock-04"
        case .editedAt: "edit-02"
        case .label: "sorting-a-z-02"
        case .custom: "custom-field"
        }
    }
}
