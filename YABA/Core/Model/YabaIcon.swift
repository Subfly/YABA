//
//  YabaIcon.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import Foundation

struct YabaIcon: Hashable, Codable {
    let name: String
    let tags: String
    let category: String
}

struct PreloadYabaIconHolder: Codable {
    let icons: [YabaIcon]
}
