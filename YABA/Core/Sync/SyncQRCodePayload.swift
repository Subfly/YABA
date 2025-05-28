//
//  SyncQRCodePayload.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import Foundation

struct SyncQRCodePayload: Codable {
    let ipAddress: String
    let port: Int
    let deviceName: String
    let platform: String // iOS, macOS, etc.
}
