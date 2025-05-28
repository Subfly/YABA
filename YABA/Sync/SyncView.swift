//
//  SyncView.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import SwiftUI

struct SyncView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @State
    private var state: SyncState = .init()
    
    var body: some View {
        NavigationView {
            viewSwitcher
                .animation(.smooth, value: state.isSyncing)
                .transition(.blurReplace)
                .navigationTitle("Synchronize Label")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button {
                            dismiss()
                        } label: {
                            YabaIconView(bundleKey: "cancel-01")
                        }
                    }
                }
        }
        .onDisappear {
            state.disconnect()
        }
    }
    
    @ViewBuilder
    private var viewSwitcher: some View {
        if state.isSyncing {
            syncingContent
        } else {
            scanningContent
        }
    }
    
    @ViewBuilder
    private var scanningContent: some View {
        VStack {
            // TODO: ADD TEXT TO INFORM SCAN
            if state.isScanning {
                QRScannerView { scannedText in
                    state.isScanning = false
                    state.handleScannedPayload(scannedText)
                }
                .frame(width: 300, height: 300)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            } else if let qrCode = state.qrImage {
                Image(uiImage: qrCode)
                    .resizable()
                    .interpolation(.none)
                    .scaledToFit()
                    .frame(width: 300, height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                ProgressView()
                    .controlSize(.large)
                    .frame(width: 300, height: 300)
            }
            
            #if !targetEnvironment(macCatalyst)
            Button {
                state.checkCameraPermission { isGranted in
                    if isGranted {
                        withAnimation {
                            state.isScanning.toggle()
                        }
                    } else {
                        // TODO: Show Toast to open settings
                    }
                }
            } label: {
                Label {
                    Text(
                        state.isScanning
                        ? LocalizedStringKey("Sync Show QR")
                        : LocalizedStringKey("Sync Show Camera")
                    )
                } icon: {
                    YabaIconView(
                        bundleKey: state.isScanning
                        ? "qr-code"
                        : "camera-01"
                    ).frame(width: 22, height: 22)
                }
            }
            .padding(.top, 16)
            #endif
        }
    }
    
    @ViewBuilder
    private var syncingContent: some View {
        
    }
}

#Preview {
    SyncView()
}
