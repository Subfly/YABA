//
//  SyncState.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import Foundation
import SwiftData
import SwiftUI

#if false
@MainActor
@Observable
internal class SyncState {
    @ObservationIgnored
    @AppStorage(Constants.deviceIdKey)
    private var deviceId: String = UUID().uuidString
    
    // Network sync manager injected from environment
    private var networkSyncManager: NetworkSyncManager?
    
    var selectedDeviceId: String?
    var isNetworkSyncRunning: Bool = false
    
    // Expose network manager properties for UI
    var discoveredDevices: [ConnectedDevice] {
        networkSyncManager?.discoveredDevices ?? []
    }
    
    var pendingSyncRequests: [SyncRequestMessage] {
        networkSyncManager?.pendingSyncRequests ?? []
    }
    
    var syncStatus: SyncStatus {
        networkSyncManager?.syncStatus ?? .idle
    }
    
    var lastError: NetworkSyncError? {
        networkSyncManager?.lastError
    }
    
    var syncingWithDevice: String? {
        networkSyncManager?.syncingWithDevice
    }
    
    var lastReceivedSyncData: SyncDataMessage? {
        networkSyncManager?.lastReceivedSyncData
    }
    
    var needsToSendOurData: Bool {
        networkSyncManager?.needsToSendOurData ?? false
    }
    
    init(networkSyncManager: NetworkSyncManager? = nil) {
        self.networkSyncManager = networkSyncManager
        if networkSyncManager != nil {
            setupSyncCallbacks()
        }
    }
    
    func setNetworkSyncManager(_ manager: NetworkSyncManager) {
        self.networkSyncManager = manager
        setupSyncCallbacks()
    }
    
    private func setupSyncCallbacks() {
        guard let networkSyncManager = networkSyncManager else { return }
        
        networkSyncManager.onSyncRequestSent = { _ in }
        networkSyncManager.onSyncRequestAccepted = { _ in }
        networkSyncManager.onSyncRequestRejected = { _ in }
        networkSyncManager.onSyncStarted = { _ in }
        networkSyncManager.onSyncCompleted = { _ in }
        networkSyncManager.onSyncFailed = { _, _ in }
        networkSyncManager.onError = { _ in }
    }
    
    func startNetworkDiscovery() async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.startDiscovery()
        isNetworkSyncRunning = true
    }
    
    func stopNetworkDiscovery() async {
        guard let networkSyncManager = networkSyncManager else { return }
        
        await networkSyncManager.stopDiscovery()
        isNetworkSyncRunning = false
    }
    
    func sendSyncRequest(to device: ConnectedDevice) async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.sendSyncRequest(to: device)
    }
    
    func acceptSyncRequest(_ request: SyncRequestMessage, using modelContext: ModelContext) async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.acceptSyncRequest(request, using: modelContext)
    }
    
    func rejectSyncRequest(_ request: SyncRequestMessage) async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.rejectSyncRequest(request)
    }
    
    func sendOurDataIfNeeded(using modelContext: ModelContext) async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.sendOurDataIfNeeded(using: modelContext)
    }
    
    func completeSyncWithIncomingData(_ syncData: SyncDataMessage, using modelContext: ModelContext) async throws {
        guard let networkSyncManager = networkSyncManager else {
            throw NetworkSyncError.invalidConfiguration
        }
        
        try await networkSyncManager.completeSyncWithIncomingData(syncData, using: modelContext)
    }
}
#endif
