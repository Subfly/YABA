//
//  NetworkSyncManager.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation
import SwiftData
import Combine
import SwiftUI
import UIKit

/// Simplified network sync manager for device discovery and sync requests
@MainActor
@Observable
@preconcurrency
final class NetworkSyncManager {
    
    // MARK: - Published State
    
    /// Whether device discovery is running
    private(set) var isDiscovering: Bool = false
    
    /// List of discovered devices available for sync
    private(set) var discoveredDevices: [ConnectedDevice] = []
    
    /// Current sync state
    private(set) var syncStatus: SyncStatus = .idle
    
    /// Last error that occurred
    private(set) var lastError: NetworkSyncError?
    
    /// Pending sync requests from other devices
    private(set) var pendingSyncRequests: [SyncRequestMessage] = []
    
    /// Current sync operation device (nil if not syncing)
    private(set) var syncingWithDevice: String?
    
    /// Flag to indicate we need to send our data after acceptance
    private(set) var needsToSendOurData: Bool = false
    
    // MARK: - Private Properties
    
    @ObservationIgnored
    @AppStorage(Constants.deviceIdKey)
    private var deviceId: String = UUID().uuidString
    
    @ObservationIgnored
    @AppStorage(Constants.deviceNameKey)
    private var deviceName: String = {
        return UIDevice.current.name
    }()
    
    private let networkService: SimpleNetworkService
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Callbacks
    
    var onSyncRequestSent: ((String) -> Void)?
    var onSyncRequestAccepted: ((String) -> Void)?
    var onSyncRequestRejected: ((String) -> Void)?
    var onSyncStarted: ((String) -> Void)?
    var onSyncCompleted: ((String) -> Void)?
    var onSyncFailed: ((String, String) -> Void)?
    var onError: ((String) -> Void)?
    
    // MARK: - Initialization
    
    init() {
        self.networkService = SimpleNetworkService()
        setupEventHandling()
    }
    
    // MARK: - Public Interface
    
    /// Start device discovery
    func startDiscovery() async throws {
        guard !isDiscovering else { return }
        
        try await networkService.startDiscovery(
            deviceId: deviceId,
            deviceName: deviceName,
            deviceType: DeviceType.current
        )
        
        isDiscovering = true
        syncStatus = .discovering
    }
    
    /// Stop device discovery
    func stopDiscovery() async {
        await networkService.stopDiscovery()
        isDiscovering = false
        discoveredDevices = []
        syncStatus = .idle
    }
    
    /// Send sync request to a device
    func sendSyncRequest(to device: ConnectedDevice) async throws {
        guard syncStatus == .discovering || syncStatus == .idle else {
            throw NetworkSyncError.deviceBusy
        }
        
        syncStatus = .requesting
        syncingWithDevice = device.deviceId
        
        let request = SyncRequestMessage(
            requestId: UUID().uuidString,
            fromDeviceId: deviceId,
            fromDeviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date()),
            message: nil
        )
        
        try await networkService.sendSyncRequest(request, to: device)
        
        // Notify that sync request was sent
        onSyncRequestSent?(device.name)
    }
    
    /// Accept a sync request (just send acceptance, don't send data yet)
    func acceptSyncRequest(_ request: SyncRequestMessage, using modelContext: ModelContext) async throws {
        // Remove from pending requests
        pendingSyncRequests.removeAll { $0.requestId == request.requestId }
        
        // Send acceptance response
        let response = SyncRequestResponse(
            requestId: request.requestId,
            accepted: true,
            fromDeviceId: deviceId,
            fromDeviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date())
        )
        
        guard let device = discoveredDevices.first(where: { $0.deviceId == request.fromDeviceId }) else {
            throw NetworkSyncError.connectionFailed("Device not found")
        }
        
        try await networkService.sendSyncResponse(response, to: device)
        
        // Set up for simultaneous data exchange (don't send data immediately)
        syncingWithDevice = device.deviceId
        syncStatus = .syncing
        needsToSendOurData = true
        
        // Notify that sync started
        onSyncStarted?(device.name)
    }
    
    /// Send our data to the device we're syncing with (called after acceptance)
    func sendOurDataIfNeeded(using modelContext: ModelContext) async throws {
        guard needsToSendOurData,
              let deviceId = syncingWithDevice,
              let device = discoveredDevices.first(where: { $0.deviceId == deviceId }) else {
            return
        }
        
        // Clear the flag first
        needsToSendOurData = false
        
        // Send our data
        try await performSync(with: device, modelContext: modelContext)
    }
    
    /// Complete sync when we have both devices' data
    func completeSyncWithIncomingData(_ syncData: SyncDataMessage, using modelContext: ModelContext) async throws {
        do {
            let dataManager = DataManager()
            
            // Create a sync request from the incoming data
            let request = SyncRequest(
                deviceId: syncData.deviceId,
                deviceName: syncData.deviceName,
                timestamp: syncData.timestamp,
                ipAddress: syncData.ipAddress,
                bookmarks: syncData.bookmarks,
                collections: syncData.collections,
                deletionLogs: syncData.deletionLogs
            )
            
            // Merge the incoming data with our local data
            let _ = try await dataManager.handleIncomingSyncRequest(
                request,
                using: modelContext,
                deviceId: deviceId,
                deviceName: deviceName,
                ipAddress: getLocalIPAddress() ?? "unknown"
            )
            
            // Clear the received data and mark sync as completed
            lastReceivedSyncData = nil
            syncStatus = .completed
            syncingWithDevice = nil
            needsToSendOurData = false
            
            // Notify that sync completed
            onSyncCompleted?(syncData.deviceName)
            
        } catch {
            syncStatus = .failed(error.localizedDescription)
            syncingWithDevice = nil
            needsToSendOurData = false
            lastReceivedSyncData = nil
            
            // Notify that sync failed
            onSyncFailed?(syncData.deviceName, error.localizedDescription)
            
            throw error
        }
    }
    
    /// Reject a sync request
    func rejectSyncRequest(_ request: SyncRequestMessage) async throws {
        // Remove from pending requests
        pendingSyncRequests.removeAll { $0.requestId == request.requestId }
        
        // Send rejection response
        let response = SyncRequestResponse(
            requestId: request.requestId,
            accepted: false,
            fromDeviceId: deviceId,
            fromDeviceName: deviceName,
            timestamp: ISO8601DateFormatter().string(from: Date())
        )
        
        guard let device = discoveredDevices.first(where: { $0.deviceId == request.fromDeviceId }) else {
            return // Device no longer available, just remove from pending
        }
        
        try await networkService.sendSyncResponse(response, to: device)
    }
    
    // MARK: - Private Methods
    
    private func setupEventHandling() {
        // Handle discovered devices
        networkService.discoveredDevicesPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] devices in
                self?.discoveredDevices = devices
            }
            .store(in: &cancellables)
        
        // Handle incoming sync requests
        networkService.syncRequestsPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] request in
                self?.handleIncomingSyncRequest(request)
            }
            .store(in: &cancellables)
        
        // Handle sync responses
        networkService.syncResponsesPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] response in
                Task { await self?.handleSyncResponse(response) }
            }
            .store(in: &cancellables)
        
        // Handle sync data
        networkService.syncDataPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] syncData in
                Task { await self?.handleIncomingSyncData(syncData) }
            }
            .store(in: &cancellables)
        
        // Handle errors
        networkService.errorsPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] error in
                self?.handleError(error)
            }
            .store(in: &cancellables)
    }
    
    private func handleIncomingSyncRequest(_ request: SyncRequestMessage) {
        // Add to pending requests for UI to show
        pendingSyncRequests.append(request)
    }
    
    private func handleIncomingSyncData(_ syncData: SyncDataMessage) async {
        // Store the sync data and let the UI handle it with proper ModelContext
        lastReceivedSyncData = syncData
    }
    
    // Property to hold received sync data for UI to process
    private(set) var lastReceivedSyncData: SyncDataMessage?
    
    private func handleSyncResponse(_ response: SyncRequestResponse) async {
        guard syncingWithDevice == response.fromDeviceId else { return }
        
        if response.accepted {
            // Other device accepted, we need to send our data to them
            guard let _ = discoveredDevices.first(where: { $0.deviceId == response.fromDeviceId }) else {
                handleError(.connectionFailed("Device not found"))
                return
            }
            
            // Notify that sync request was accepted
            onSyncRequestAccepted?(response.fromDeviceName)
            
            // Set flag so UI can trigger sending our data
            needsToSendOurData = true
            syncStatus = .syncing
            onSyncStarted?(response.fromDeviceName)
            
        } else {
            // Other device rejected
            syncStatus = .idle  // Set to idle so user can send new requests (error toast will show the rejection)
            syncingWithDevice = nil
            needsToSendOurData = false
            
            // Notify that sync request was rejected
            onSyncRequestRejected?(response.fromDeviceName)
        }
    }
    
    private func performSync(with device: ConnectedDevice, modelContext: ModelContext) async throws {
        syncStatus = .syncing
        syncingWithDevice = device.deviceId
        
        // Notify that sync started
        onSyncStarted?(device.name)
        
        do {
            // Use DataManager to prepare sync data
            let dataManager = DataManager()
            let syncData = try dataManager.prepareSyncData(
                using: modelContext,
                deviceId: deviceId,
                deviceName: deviceName,
                ipAddress: getLocalIPAddress() ?? "unknown"
            )
            
            // Send our data to the other device
            let message = SyncDataMessage(
                deviceId: syncData.deviceId,
                deviceName: syncData.deviceName,
                timestamp: syncData.timestamp,
                ipAddress: syncData.ipAddress,
                bookmarks: syncData.bookmarks,
                collections: syncData.collections,
                deletionLogs: syncData.deletionLogs
            )
            
            try await networkService.sendSyncData(message, to: device)
            
            // The other device will process our data and send their data back
            // When we receive their data, handleIncomingSyncData will be called
            // For now, we stay in syncing state until we get their response
            
        } catch {
            syncStatus = .failed(error.localizedDescription)
            syncingWithDevice = nil
            
            // Notify that sync failed
            onSyncFailed?(device.name, error.localizedDescription)
            
            throw error
        }
    }
    
    private func handleError(_ error: NetworkSyncError) {
            lastError = error
        
        if syncStatus == .syncing || syncStatus == .requesting {
            syncStatus = .failed(error.localizedDescription)
            syncingWithDevice = nil
        }
        
        // Notify about the error
        onError?(error.localizedDescription)
    }
    
    private func getLocalIPAddress() -> String? {
        // Simple IP address detection
        var address: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        
        if getifaddrs(&ifaddr) == 0 {
            var ptr = ifaddr
            while ptr != nil {
                defer { ptr = ptr?.pointee.ifa_next }
                
                let interface = ptr?.pointee
                let addrFamily = interface?.ifa_addr.pointee.sa_family
                
                if addrFamily == UInt8(AF_INET) || addrFamily == UInt8(AF_INET6) {
                    guard let interfaceName = interface?.ifa_name,
                          let interfaceAddr = interface?.ifa_addr else { continue }
                    
                    let name = String(cString: interfaceName)
                    if name == "en0" || name == "en1" || name.hasPrefix("wlan") {
                        var addr = interfaceAddr.pointee
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        getnameinfo(&addr, socklen_t(addr.sa_len), &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST)
                        address = String(cString: hostname)
                        break
                    }
                }
            }
            freeifaddrs(ifaddr)
        }
        
        return address
    }
}

extension EnvironmentValues {
    @Entry var networkSyncManager: NetworkSyncManager = .init()
}
