//
//  SyncState.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import AVFoundation
import CoreImage.CIFilterBuiltins
import Foundation
import SwiftUI
import SwiftData

internal enum Mode {
    case client, server
}

@MainActor
@Observable
internal class SyncState {
    @ObservationIgnored
    @AppStorage(Constants.deviceIdKey)
    private var deviceId: String = UUID().uuidString
    
    @ObservationIgnored
    private var syncServer: SyncServer = .init()
    
    @ObservationIgnored
    private var syncClient: SyncClient = .init()

    let modelContext: ModelContext
    
    var qrImage: UIImage?
    var isSyncing: Bool = false
    var mode: Mode = .server

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
        if let ip = getLocalIPAddress() {
            generateQRCode(ip: ip)
            syncServer.setIp(ip)
            Task {
                try await syncServer.startServer()
            }
        }
    }

    // MARK: SYNC RELATED ---
    
    func changeMode() {
        mode = switch mode {
        case .client: .server
        case .server: .client
        }
        
        Task {
            if mode == .server {
                try await syncServer.startServer()
            } else {
                try await syncServer.stopServer()
            }
        }
    }

    func handleScannedPayload(_ string: String) {
        guard mode == .client else {
            print("❌ Ignored scanned QR because device is not in client mode")
            return
        }

        guard let data = string.data(using: .utf8),
              let payload = try? JSONDecoder().decode(SyncQRCodePayload.self, from: data) else {
            print("❌ Invalid QR Code")
            return
        }

        print("📶 Scanned IP: \(payload.ipAddress), Port: \(payload.port)")
        Task {
            syncClient.setUp(with: payload.ipAddress)
            syncClient.connect()
            await startSyncing(with: payload)
        }
    }
    
    func stopServer() {
        if mode == .server {
            Task {
                try await syncServer.stopServer()
            }
        } else {
            syncClient.disconnect()
        }
    }

    private func startSyncing(with payload: SyncQRCodePayload) async {
        isSyncing = true
        defer { isSyncing = false }
    }
    
    // MARK: QR RELATED ---

    func generateQRCode(ip: String) {
        let payload = SyncQRCodePayload(ipAddress: ip, port: Constants.port, deviceId: deviceId)

        guard let data = try? JSONEncoder().encode(payload) else {
            print("❌ Failed to encode QR payload")
            return
        }

        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()

        filter.message = data
        filter.correctionLevel = "H"

        if let outputImage = filter.outputImage,
           let cgImage = context.createCGImage(outputImage, from: outputImage.extent) {
            qrImage = UIImage(cgImage: cgImage)
        } else {
            print("❌ Failed to generate QR code")
        }
    }

    func checkCameraPermission(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async { completion(granted) }
            }
        case .denied, .restricted:
            completion(false)
        @unknown default:
            completion(false)
        }
    }

    private func getLocalIPAddress() -> String? {
        var address: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return nil }
        defer { freeifaddrs(ifaddr) }

        var ptr = ifaddr
        while ptr != nil {
            let interface = ptr!.pointee
            let addrFamily = interface.ifa_addr.pointee.sa_family
            if addrFamily == UInt8(AF_INET) {
                let name = String(cString: interface.ifa_name)
                if name.contains("en") {
                    var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                                &hostname, socklen_t(hostname.count),
                                nil, socklen_t(0), NI_NUMERICHOST)
                    address = String(cString: hostname)
                    break
                }
            }
            ptr = interface.ifa_next
        }

        return address
    }
}
