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
            "bookmark-02"
        case .folder:
            "folder-01"
        case .tag:
            "tag-01"
        default:
            "plus-sign"
        }
    }
}
