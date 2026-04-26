//
//  NetworkSyncError.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation

/// Network synchronization specific errors
enum NetworkSyncError: LocalizedError, Equatable {
    case serviceUnavailable
    case invalidConfiguration
    case connectionFailed(String)
    case syncTimeout
    case dataTransferFailed(String)
    case authenticationFailed
    case incompatibleVersion(String)
    case deviceBusy
    case lockingFailed
    case unknown(String)
    
    var errorDescription: String? {
        switch self {
        case .serviceUnavailable:
            return "Network synchronization service is not available"
        case .invalidConfiguration:
            return "Invalid network configuration"
        case .connectionFailed(let reason):
            return "Connection failed: \(reason)"
        case .syncTimeout:
            return "Synchronization timed out"
        case .dataTransferFailed(let reason):
            return "Data transfer failed: \(reason)"
        case .authenticationFailed:
            return "Device authentication failed"
        case .incompatibleVersion(let version):
            return "Incompatible sync protocol version: \(version)"
        case .deviceBusy:
            return "Device is currently busy with another sync operation"
        case .lockingFailed:
            return "Failed to acquire sync lock"
        case .unknown(let message):
            return "Unknown error: \(message)"
        }
    }
}
