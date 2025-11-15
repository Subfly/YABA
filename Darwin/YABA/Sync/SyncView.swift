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
    
    @Environment(\.networkSyncManager)
    private var networkSyncManager
    
    @Environment(\.modelContext)
    private var modelContext
    
    @AppStorage(Constants.deviceNameKey)
    private var deviceName: String = ""
    
    @State
    private var state: SyncState = .init()
    
    // Alert state for sync requests
    @State
    private var showingSyncRequestAlert = false
    
    @State
    private var currentSyncRequest: SyncRequestMessage?
    
    // Syncing state
    @State
    private var sendingRequestToDeviceId: String?
    
    var body: some View {
        NavigationView {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                #if !targetEnvironment(macCatalyst)
                if UIDevice.current.userInterfaceIdiom == .pad {
                    content.padding(150)
                } else {
                    content
                }
                #else
                content
                #endif
            }
            .navigationTitle("Synchronize Label")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Text("Done")
                    }
                }
            }
            .alert("Sync Request Title", isPresented: $showingSyncRequestAlert) {
                Button("Accept") {
                    Task {
                        if let request = currentSyncRequest {
                            do {
                                try await state.acceptSyncRequest(request, using: modelContext)
                            } catch {
                                #if DEBUG
                                print("Failed to accept sync request: \(error)")
                                #endif
                            }
                        }
                        currentSyncRequest = nil
                    }
                }
                
                Button("Reject", role: .cancel) {
                    Task {
                        if let request = currentSyncRequest {
                            do {
                                try await state.rejectSyncRequest(request)
                            } catch {
                                #if DEBUG
                                print("Failed to reject sync request: \(error)")
                                #endif
                            }
                        }
                        currentSyncRequest = nil
                    }
                }
            } message: {
                if let request = currentSyncRequest {
                    Text("Sync Request Message \(request.fromDeviceName)")
                }
            }
            .toast(
                state: state.toastManager.toastState,
                isShowing: state.toastManager.isShowing,
                onDismiss: {
                    state.toastManager.hide()
                }
            )
            .onChange(of: state.pendingSyncRequests) { _, newRequests in
                if let latestRequest = newRequests.first, currentSyncRequest == nil {
                    currentSyncRequest = latestRequest
                    showingSyncRequestAlert = true
                }
            }
            .onChange(of: state.needsToSendOurData) { _, needsToSend in
                if needsToSend {
                    Task {
                        do {
                            try await state.sendOurDataIfNeeded(using: modelContext)
                        } catch {
                            #if DEBUG
                            print("Failed to send our data: \(error)")
                            #endif
                        }
                    }
                }
            }
            .onChange(of: state.lastReceivedSyncData) { _, newSyncData in
                if let syncData = newSyncData {
                    Task {
                        do {
                            try await state.completeSyncWithIncomingData(syncData, using: modelContext)
                        } catch {
                            #if DEBUG
                            print("Failed to complete sync with incoming data: \(error)")
                            #endif
                        }
                    }
                }
            }
            .onDisappear {
                Task {
                    await state.stopNetworkDiscovery()
                }
            }
            .onAppear {
                state.setNetworkSyncManager(networkSyncManager)
                Task {
                    try? await state.startNetworkDiscovery()
                }
            }
        }
    }
    
    @ViewBuilder
    private var content: some View {
        List {
            Section {
                if state.discoveredDevices.isEmpty {
                    ContentUnavailableView {
                        Label {
                            Text("Sync Searching For Devices Title")
                        } icon: {
                            YabaIconView(bundleKey: "search-01")
                                .scaledToFit()
                                .frame(width: 52, height: 52)
                        }
                    } description: {
                        Text("Sync Searching For Devices Description")
                    }
                } else {
                    ForEach(state.discoveredDevices) { device in
                        DiscoveredDeviceItem(
                            device: device,
                            isSendingRequest: sendingRequestToDeviceId == device.deviceId,
                            isSyncing: state.syncingWithDevice == device.deviceId,
                            isDisabled: state.syncingWithDevice != nil
                        ) {
                            Task {
                                sendingRequestToDeviceId = device.deviceId
                                do {
                                    try await state.sendSyncRequest(to: device)
                                } catch {
                                    #if DEBUG
                                    print("Failed to send sync request to \(device.name): \(error)")
                                    #endif
                                }
                                sendingRequestToDeviceId = nil
                            }
                        }
                    }
                }
            } header: {
                Label {
                    Text("Sync Available Devices Title")
                } icon: {
                    YabaIconView(bundleKey: "search-01")
                        .frame(width: 18, height: 18)
                }
            }
            
            Section {
                Text("Sync Description \(deviceName)")
            } header: {
                Label {
                    Text("Info")
                } icon: {
                    YabaIconView(bundleKey: "information-circle")
                        .frame(width: 18, height: 18)
                }
            }
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
    }
    
    private func isFailed(_ syncStatus: SyncStatus) -> Bool {
        if case .failed = syncStatus {
            return true
        }
        return false
    }
}

private struct DiscoveredDeviceItem: View {
    let device: ConnectedDevice
    let isSendingRequest: Bool
    let isSyncing: Bool
    let isDisabled: Bool
    let onSendRequest: () -> Void
    
    var body: some View {
        Button {
            if !isSendingRequest && !isSyncing && !isDisabled {
                onSendRequest()
            }
        } label: {
            HStack {
                YabaIconView(bundleKey: device.deviceType.symbolName)
                    .frame(width: 36, height: 36)
                    .foregroundStyle(iconColor)
                VStack(alignment: .leading) {
                    Text(device.name)
                        .font(.title3)
                        .fontWeight(.semibold)
                    Text(statusText)
                        .font(.subheadline)
                        .foregroundStyle(statusColor)
                }
                Spacer()
                
                if isSendingRequest || isSyncing {
                    ProgressView()
                        .scaleEffect(0.8)
                } else {
                    YabaIconView(bundleKey: "database-sync-01")
                        .frame(width: 22, height: 22)
                        .foregroundStyle(.tint)
                }
            }
        }
        .disabled(isDisabled)
        .swipeActions {
            Button {
                onSendRequest()
            } label: {
                VStack {
                    YabaIconView(bundleKey: "database-sync-01")
                    Text("Sync")
                }
            }.tint(.orange)
        }
    }
    
    private var iconColor: Color {
        if isDisabled && !isSyncing {
            return .gray
        } else if isSyncing {
            return .orange
        } else {
            return .accentColor
        }
    }
    
    private var statusText: LocalizedStringKey {
        if isSendingRequest {
            return "Sync Sending Request Label"
        } else if isSyncing {
            return "Sync Syncing Label"
        } else {
            return device.deviceType.displayName
        }
    }
    
    private var statusColor: Color {
        if isSendingRequest {
            return .blue
        } else if isSyncing {
            return .orange
        } else {
            return .secondary
        }
    }
}

#Preview {
    SyncView()
}
