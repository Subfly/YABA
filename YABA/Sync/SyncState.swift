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

@MainActor
@Observable
internal class SyncState {
    @ObservationIgnored
    private let syncServer: SyncServer = .init()
    
    @ObservationIgnored
    private let syncClient: SyncClient = .init()
    
    let toastManager: ToastManager = .init()
    
    var qrImage: UIImage?
    var isScanning: Bool = false
    var isSyncing: Bool = false
    
    init() {
        if let ip = syncServer.getLocalIPAddress() {
            generateQRCode(ip: ip, port: 8888)
        } else {
            // TODO: SHOW TOAST
        }
    }
    
    func generateQRCode(ip: String, port: Int) {
        let payload = SyncQRCodePayload(
            ipAddress: ip,
            port: port,
            deviceName: UIDevice.current.name,
            platform: UIDevice.current.systemName
        )
        
        guard let data = try? JSONEncoder().encode(payload) else {
            // TODO: SHOW TOAST
            return
        }
        
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        
        filter.message = data
        filter.correctionLevel = "H"
        
        if let outputImage = filter.outputImage {
            if let cgImage = context.createCGImage(outputImage, from: outputImage.extent) {
                qrImage =  UIImage(cgImage: cgImage)
                return
            }
        }
        // TODO: SHOW TOAST
    }
    
    func checkCameraPermission(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    completion(granted)
                }
            }
        case .denied, .restricted:
            completion(false)
        @unknown default:
            completion(false)
        }
    }
    
    func handleScannedPayload(_ string: String) {
        guard let data = string.data(using: .utf8),
              let payload = try? JSONDecoder().decode(SyncQRCodePayload.self, from: data) else {
            // TODO: SHOW INVALID QR
            return
        }
        
        print("Scanned IP: \(payload.ipAddress), Port: \(payload.port)")
        startSyncing(with: payload)
    }
    
    func startSyncing(with payload: SyncQRCodePayload) {
        isSyncing = true
        defer { isSyncing = false }
        
        Task {
            do {
                try syncClient.connect(to: payload)
                // TODO: SHOW CONNECTED TOAST
                // TODO: Begin sync process here — replace with real logic:
            } catch {
                // TODO: SHOW FAILED CONNECTION TOAST
            }
        }
    }
    
    func disconnect() {
        syncClient.disconnect()
    }
}
