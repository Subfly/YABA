//
//  ConnectedDevice.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation

/// Information about a discovered device available for sync
struct ConnectedDevice: Codable, Identifiable, Equatable {
    let id: String // deviceId
    let name: String
    let ipAddress: String
    let port: Int
    let deviceType: DeviceType
    let lastSeen: Date
    
    var deviceId: String { id }
}
