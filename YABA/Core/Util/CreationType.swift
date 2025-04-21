//
//  CreationType.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

enum CreationType {
    case bookmark, folder, tag, main

    func getIcon() -> String {
        switch self {
        case .bookmark:
            "bookmark"
        case .folder:
            "folder"
        case .tag:
            "tag"
        default:
            "plus"
        }
    }
}
