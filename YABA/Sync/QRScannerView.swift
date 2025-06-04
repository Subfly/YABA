//
//  QRScannerView.swift
//  YABA
//
//  Created by Ali Taha on 28.05.2025.
//


import SwiftUI
import AVFoundation

#if !os(visionOS)
struct QRScannerView: UIViewRepresentable {
    let onQRCodeFound: (String) -> Void
    
    func makeCoordinator() -> Coordinator {
        return Coordinator(onQRCodeFound: onQRCodeFound)
    }
    
    func makeUIView(context: Context) -> CameraPreviewView {
        let view = CameraPreviewView()
        context.coordinator.setupCamera(previewLayer: view.previewLayer)
        return view
    }
    
    func updateUIView(_ uiView: CameraPreviewView, context: Context) {
        // Nothing to update dynamically
    }

    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        private let session = AVCaptureSession()
        private let onQRCodeFound: (String) -> Void
        private var didDetectQRCode = false
        
        init(onQRCodeFound: @escaping (String) -> Void) {
            self.onQRCodeFound = onQRCodeFound
        }
        
        func setupCamera(previewLayer: AVCaptureVideoPreviewLayer?) {
            guard let videoCaptureDevice = AVCaptureDevice.default(for: .video),
                  let videoInput = try? AVCaptureDeviceInput(device: videoCaptureDevice),
                  session.canAddInput(videoInput)
            else {
                return
            }

            session.addInput(videoInput)

            let metadataOutput = AVCaptureMetadataOutput()
            if session.canAddOutput(metadataOutput) {
                session.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]
            }

            previewLayer?.session = session
            previewLayer?.videoGravity = .resizeAspectFill

            Task {
                session.startRunning()
            }
        }
        
        func metadataOutput(_ output: AVCaptureMetadataOutput,
                            didOutput metadataObjects: [AVMetadataObject],
                            from connection: AVCaptureConnection) {
            guard !didDetectQRCode else { return }
            
            if let metadataObject = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
               metadataObject.type == .qr,
               let stringValue = metadataObject.stringValue {
                didDetectQRCode = true
                session.stopRunning()
                onQRCodeFound(stringValue)
            }
        }
    }
}

final class CameraPreviewView: UIView {
    var previewLayer: AVCaptureVideoPreviewLayer? {
        layer as? AVCaptureVideoPreviewLayer
    }

    override class var layerClass: AnyClass {
        return AVCaptureVideoPreviewLayer.self
    }
}
#endif
