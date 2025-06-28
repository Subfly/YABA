//
//  SyncQRCodePayload.swift
//  YABA
//
//  Created by Ali Taha on 31.05.2025.
//

import Foundation

struct SyncQRCodePayload: Codable {
    let ipAddress: String
    let port: Int
    let deviceId: String
}
