//
//  NetworkSyncService.swift
//  YABA
//
//  Created by Ali Taha on 17.08.2025.
//

import Foundation
import Combine
import Network
import SwiftUI

/// Simple network service for device discovery and sync communication
/// Uses cross-platform compatible networking (no Apple-specific APIs)
final class SimpleNetworkService: NSObject {
    private let serviceType = "_yaba-sync._tcp."
    
    // MARK: - Publishers
    
    let discoveredDevicesPublisher = CurrentValueSubject<[ConnectedDevice], Never>([])
    let syncRequestsPublisher = PassthroughSubject<SyncRequestMessage, Never>()
    let syncResponsesPublisher = PassthroughSubject<SyncRequestResponse, Never>()
    let syncDataPublisher = PassthroughSubject<SyncDataMessage, Never>()
    let errorsPublisher = PassthroughSubject<NetworkSyncError, Never>()
    
    // MARK: - Private Properties
    
    private var isRunning = false
    private var currentDeviceId: String?
    private var currentDeviceName: String?
    private var currentDeviceType: DeviceType?
    
    // Network service discovery
    private var netServiceBrowser: NetServiceBrowser?
    private var netService: NetService?
    private var tcpListener: NWListener?
    
    // Discovered services and devices
    private var discoveredServices: [NetService] = []
    private var connectedDevices: [String: ConnectedDevice] = [:]
    
    // MARK: - Public Interface
    
    /// Start device discovery
    func startDiscovery(deviceId: String, deviceName: String, deviceType: DeviceType) async throws {
        guard !isRunning else { return }
        
        currentDeviceId = deviceId
        currentDeviceName = deviceName
        currentDeviceType = deviceType
        
        // Start publishing our service
        try await startPublishingService()
        
        // Start discovering other services
        startBrowsingServices()
        
        isRunning = true
    }
    
    /// Stop device discovery
    func stopDiscovery() async {
        guard isRunning else { return }
        
        // Stop publishing
        netService?.stop()
        netService = nil
        
        // Stop browsing
        netServiceBrowser?.stop()
        netServiceBrowser = nil
        
        // Stop TCP listener
        tcpListener?.cancel()
        tcpListener = nil
        
        // Clear discovered devices
        discoveredServices.removeAll()
        connectedDevices.removeAll()
        discoveredDevicesPublisher.send([])
        
        isRunning = false
    }
    
    /// Send sync request to a device
    func sendSyncRequest(_ request: SyncRequestMessage, to device: ConnectedDevice) async throws {
        try await sendMessage(request, to: device)
    }
    
    /// Send sync response to a device
    func sendSyncResponse(_ response: SyncRequestResponse, to device: ConnectedDevice) async throws {
        try await sendMessage(response, to: device)
    }
    
    /// Send sync data to a device
    func sendSyncData(_ data: SyncDataMessage, to device: ConnectedDevice) async throws {
        try await sendMessage(data, to: device)
    }
    
    // MARK: - Private Methods
    
    private func startPublishingService() async throws {
        let parameters = NWParameters.tcp
            parameters.allowLocalEndpointReuse = true

            tcpListener = try NWListener(using: parameters)
            guard let listener = tcpListener else { throw NetworkSyncError.serviceUnavailable }

            listener.newConnectionHandler = { [weak self] conn in
                Task { await self?.handleIncomingConnection(conn) }
            }

            // Wait until the listener is ready and has a port
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                listener.stateUpdateHandler = { state in
                    switch state {
                    case .ready:
                        continuation.resume()
                    case .failed(let err):
                        continuation.resume(throwing: err)
                    default:
                        break
                    }
                }
                listener.start(queue: .global())
            }

            guard let port = listener.port else {
                throw NetworkSyncError.serviceUnavailable
            }

            // publish with the exact serviceType const
            netService = NetService(
                domain: "local.",
                type: serviceType,
                name: currentDeviceName ?? "YABA Device",
                port: Int32(port.rawValue)
            )
            netService?.includesPeerToPeer = true
            netService?.delegate = self
            netService?.setTXTRecord(createTXTRecord())
            netService?.publish()
    }
    
    private func startBrowsingServices() {
        DispatchQueue.main.async {
            self.netServiceBrowser = NetServiceBrowser()
            self.netServiceBrowser?.includesPeerToPeer = true
            self.netServiceBrowser?.delegate = self
            self.netServiceBrowser?.searchForServices(ofType: self.serviceType, inDomain: "local.")
        }
    }
    
    private func createTXTRecord() -> Data {
        var txtRecord: [String: Data] = [:]
        
        if let deviceId = currentDeviceId {
            txtRecord["deviceId"] = deviceId.data(using: String.Encoding.utf8)
        }
        
        if let deviceType = currentDeviceType {
            txtRecord["deviceType"] = deviceType.rawValue.data(using: String.Encoding.utf8)
        }
        
        txtRecord["version"] = "1.0".data(using: String.Encoding.utf8)
        
        return NetService.data(fromTXTRecord: txtRecord)
    }
    
    private func handleIncomingConnection(_ connection: NWConnection) async {
        connection.start(queue: DispatchQueue.global())
        
        // Read messages with length prefix
        await readCompleteMessage(from: connection)
    }
    
    private func readCompleteMessage(from connection: NWConnection) async {
        // First, read the message length (4 bytes)
        connection.receive(minimumIncompleteLength: 4, maximumLength: 4) { [weak self] lengthData, _, isComplete, error in
            guard let lengthData = lengthData, lengthData.count == 4, error == nil else {
                connection.cancel()
                return
            }
            
            // Convert bytes to UInt32 length
            let messageLength = lengthData.withUnsafeBytes { $0.load(as: UInt32.self) }
            
            // Now read the actual message
            connection.receive(minimumIncompleteLength: Int(messageLength), maximumLength: Int(messageLength)) { [weak self] messageData, _, messageComplete, messageError in
                guard let messageData = messageData, messageData.count == messageLength, messageError == nil else {
                    connection.cancel()
                    return
                }
                
                // Process the complete message
                Task { await self?.handleIncomingData(messageData) }
                
                // Continue reading more messages
                if !isComplete && !messageComplete {
                    Task { await self?.readCompleteMessage(from: connection) }
                } else {
                    connection.cancel()
                }
            }
        }
    }
    
    private func handleIncomingData(_ data: Data) async {
        // First, decode to get the message type using a more flexible approach
        guard let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
              let messageTypeString = json["messageType"] as? String,
              let messageType = SyncMessageType(rawValue: messageTypeString) else {
            
            // Fallback: Try to decode using the old sequential method
            if let syncRequest = try? JSONDecoder().decode(SyncRequestMessage.self, from: data) {
                #if DEBUG
                print("Successfully decoded as SyncRequestMessage")
                #endif
                syncRequestsPublisher.send(syncRequest)
            } else if let syncResponse = try? JSONDecoder().decode(SyncRequestResponse.self, from: data) {
                #if DEBUG
                print("Successfully decoded as SyncRequestResponse")
                #endif
                syncResponsesPublisher.send(syncResponse)
            } else if let syncData = try? JSONDecoder().decode(SyncDataMessage.self, from: data) {
                #if DEBUG
                print("Successfully decoded as SyncDataMessage")
                #endif
                syncDataPublisher.send(syncData)
            } else {
                #if DEBUG
                print("Failed to decode with fallback method as well")
                #endif
            }
            return
        }
        
        // Now decode the specific message type based on the discriminator
        switch messageType {
        case .syncRequest:
            if let syncRequest = try? JSONDecoder().decode(SyncRequestMessage.self, from: data) {
                syncRequestsPublisher.send(syncRequest)
            }
        case .syncResponse:
            if let syncResponse = try? JSONDecoder().decode(SyncRequestResponse.self, from: data) {
                syncResponsesPublisher.send(syncResponse)
            }
        case .syncData:
            if let syncData = try? JSONDecoder().decode(SyncDataMessage.self, from: data) {
                syncDataPublisher.send(syncData)
            }
        }
    }
    
    private func sendMessage<T: Codable>(_ message: T, to device: ConnectedDevice) async throws {
        let messageData = try JSONEncoder().encode(message)
        
        // Create length prefix (4 bytes)
        let messageLength = UInt32(messageData.count)
        let lengthData = withUnsafeBytes(of: messageLength) { Data($0) }
        
        // Combine length prefix + message data
        var completeData = Data()
        completeData.append(lengthData)
        completeData.append(messageData)
        
        // Create connection to the device using the correct port
        let endpoint = NWEndpoint.hostPort(
            host: NWEndpoint.Host(device.ipAddress),
            port: NWEndpoint.Port(integerLiteral: UInt16(device.port))
        )
        
        let connection = NWConnection(to: endpoint, using: .tcp)
        connection.start(queue: DispatchQueue.global())
        
        // Wait a bit for connection to establish
        try await Task.sleep(for: .milliseconds(500))
        
        // Send the complete data (length + message)
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            connection.send(content: completeData, completion: .contentProcessed { _ in
                connection.cancel()
                continuation.resume()
            })
        }
    }
}

// MARK: - NetServiceBrowserDelegate

extension SimpleNetworkService: NetServiceBrowserDelegate {
    func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        // Resolve the service to get its address
        service.delegate = self
        service.resolve(withTimeout: 5.0)
        
        if !discoveredServices.contains(where: { $0.name == service.name }) {
            discoveredServices.append(service)
        }
    }
    
    func netServiceBrowser(_ browser: NetServiceBrowser, didRemove service: NetService, moreComing: Bool) {
        discoveredServices.removeAll { $0.name == service.name }
        
        // Extract device ID from service and remove from connected devices
        if let deviceId = extractDeviceId(from: service) {
            connectedDevices.removeValue(forKey: deviceId)
            updateDiscoveredDevices()
        } else {
            // Fallback: TXT record is empty during disconnection, but service.name is still available
            // Find device by matching service name with device name
            if let deviceToRemove = connectedDevices.values.first(where: { $0.name == service.name }) {
                connectedDevices.removeValue(forKey: deviceToRemove.id)
                updateDiscoveredDevices()
            }
        }
    }
}

// MARK: - NetServiceDelegate

extension SimpleNetworkService: NetServiceDelegate {
    func netServiceDidResolveAddress(_ sender: NetService) {
        guard let deviceId = extractDeviceId(from: sender),
              deviceId != currentDeviceId, // Don't include ourselves
              let ipAddress = extractIPAddress(from: sender),
              let deviceType = extractDeviceType(from: sender) else {
            return
        }
        
        let device = ConnectedDevice(
            id: deviceId,
            name: sender.name,
            ipAddress: ipAddress,
            port: Int(sender.port),
            deviceType: deviceType,
            lastSeen: Date()
        )
        
        connectedDevices[deviceId] = device
        updateDiscoveredDevices()
    }
    
    private func extractDeviceId(from service: NetService) -> String? {
        guard let txtData = service.txtRecordData() else {
            return nil
        }
        
        let txtRecord = NetService.dictionary(fromTXTRecord: txtData)
        
        guard let deviceIdData = txtRecord["deviceId"] else {
            return nil
        }
        
        return String(data: deviceIdData, encoding: String.Encoding.utf8)
    }
    
    private func extractDeviceType(from service: NetService) -> DeviceType? {
        guard let txtData = service.txtRecordData() else {
            return .unknown
        }
        
        let txtRecord = NetService.dictionary(fromTXTRecord: txtData)
        guard let deviceTypeData = txtRecord["deviceType"],
              let deviceTypeString = String(data: deviceTypeData, encoding: String.Encoding.utf8) else {
            return .unknown
        }
        
        return DeviceType(rawValue: deviceTypeString) ?? .unknown
    }
    
    private func extractIPAddress(from service: NetService) -> String? {
        // Get the first IPv4 address from the service addresses
        for address in service.addresses ?? [] {
            let data = address
            var storage = sockaddr_storage()
            data.withUnsafeBytes { bytes in
                guard bytes.count <= MemoryLayout<sockaddr_storage>.size else { return }
                withUnsafeMutableBytes(of: &storage) { storageBytes in
                    storageBytes.copyMemory(from: bytes)
                }
            }
            
            if storage.ss_family == sa_family_t(AF_INET) {
                var addr = withUnsafeBytes(of: storage) { $0.bindMemory(to: sockaddr_in.self).first! }
                let ip = withUnsafeBytes(of: &addr.sin_addr) { bytes in
                    return bytes.bindMemory(to: UInt8.self)
                }
                return "\(ip[0]).\(ip[1]).\(ip[2]).\(ip[3])"
            }
        }
        return nil
    }
    
    private func updateDiscoveredDevices() {
        let devices = Array(connectedDevices.values).sorted { $0.name < $1.name }
        discoveredDevicesPublisher.send(devices)
    }
}
