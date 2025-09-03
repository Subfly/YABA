//
//  SyncState.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import Foundation
import SwiftData
import SwiftUI

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
    
    let toastManager: ToastManager = ToastManager()
    
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
        
        networkSyncManager.onSyncRequestSent = { [weak self] deviceName in
            self?.toastManager.show(
                message: "Sync Request Sent Message \(deviceName)",
                contentColor: .primary,
                accentColor: .blue,
                iconType: .hint,
                duration: .short
            )
        }
        
        networkSyncManager.onSyncRequestAccepted = { [weak self] deviceName in
            self?.toastManager.show(
                message: "Sync Request Accepted Message \(deviceName)",
                contentColor: .primary,
                accentColor: .green,
                iconType: .success,
                duration: .short
            )
        }
        
        networkSyncManager.onSyncRequestRejected = { [weak self] deviceName in
            self?.toastManager.show(
                message: "Sync Request Rejected Message \(deviceName)",
                contentColor: .primary,
                accentColor: .red,
                iconType: .error,
                duration: .short
            )
        }
        
        networkSyncManager.onSyncStarted = { [weak self] deviceName in
            self?.toastManager.show(
                message: "Syncing With Device Message \(deviceName)",
                contentColor: .primary,
                accentColor: .orange,
                iconType: .hint,
                duration: .long
            )
        }
        
        networkSyncManager.onSyncCompleted = { [weak self] deviceName in
            self?.toastManager.show(
                message: "Sync Complete Message \(deviceName)",
                contentColor: .primary,
                accentColor: .green,
                iconType: .success,
                duration: .short
            )
        }
        
        networkSyncManager.onSyncFailed = { [weak self] deviceName, error in
            self?.toastManager.show(
                message: "Sync Failed Message \(deviceName): \(error)",
                contentColor: .primary,
                accentColor: .red,
                iconType: .error,
                duration: .short
            )
        }
        
        networkSyncManager.onError = { [weak self] error in
            self?.toastManager.show(
                message: "Sync Error Message \(error)",
                contentColor: .primary,
                accentColor: .red,
                iconType: .error,
                duration: .short
            )
        }
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
