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
    
    @Environment(\.modelContext)
    private var modelContext
    
    @State
    private var state: SyncState?
    
    var body: some View {
        NavigationView {
            viewSwitcher
                .animation(.smooth, value: state?.isSyncing == true)
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
            state?.stopServer()
        }
        .onAppear {
            state = .init(modelContext: modelContext)
        }
    }
    
    @ViewBuilder
    private var viewSwitcher: some View {
        if let state {
            if state.isSyncing {
                syncingContent
            } else {
                scanningContent
            }
        }
    }
    
    @ViewBuilder
    private var scanningContent: some View {
        if let state {
            VStack {
                // TODO: ADD TEXT TO INFORM SCAN
                switch state.mode {
                case .client:
                    QRScannerView { scannedText in
                        state.handleScannedPayload(scannedText)
                    }
                    .frame(width: 300, height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                case .server:
                    if let qrCode = state.qrImage {
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
                }
                
            #if !targetEnvironment(macCatalyst)
                Button {
                    state.checkCameraPermission { isGranted in
                        if isGranted {
                            withAnimation {
                                state.changeMode()
                            }
                        } else {
                            // TODO: Show Toast to open settings
                        }
                    }
                } label: {
                    Label {
                        Text(
                            state.mode == .client
                            ? LocalizedStringKey("Sync Show QR")
                            : LocalizedStringKey("Sync Show Camera")
                        )
                    } icon: {
                        YabaIconView(
                            bundleKey: state.mode == .client
                            ? "qr-code"
                            : "camera-01"
                        ).frame(width: 22, height: 22)
                    }
                }
                .padding(.top, 16)
            #endif
            }
        }
    }
    
    @ViewBuilder
    private var syncingContent: some View {
        
    }
}

#Preview {
    SyncView()
}
