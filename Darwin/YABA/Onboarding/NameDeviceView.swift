//
//  NameDeviceView.swift
//  YABA
//
//  Created by Ali Taha on 5.09.2025.
//

import SwiftUI

struct NameDeviceView: View {
    @AppStorage(Constants.deviceNameKey)
    private var deviceName: String = ""
    
    let onDone: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                VStack {
                    #if targetEnvironment(macCatalyst)
                    Spacer()
                    #else
                    if UIDevice.current.userInterfaceIdiom == .pad {
                        Spacer()
                    }
                    #endif
                    List {
                        Text("Name Device Message")
                        TextField(
                            "",
                            text: $deviceName,
                            prompt: Text("Name Device Prompt")
                        )
                        .safeAreaInset(edge: .leading) {
                            YabaIconView(bundleKey: DeviceType.current.symbolName)
                                .scaledToFit()
                                .frame(width: 20, height: 20)
                                .foregroundStyle(.tint)
                        }
                    }
                    .listStyle(.sidebar)
                    .listRowSeparator(.hidden)
                    .listRowSpacing(12)
                    .scrollContentBackground(.hidden)
                    #if targetEnvironment(macCatalyst)
                    Spacer()
                    #else
                    if UIDevice.current.userInterfaceIdiom == .pad {
                        Spacer()
                    }
                    #endif
                }
            }
            .navigationTitle("Name Device Title")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        onDone()
                    } label: {
                        Text("Done")
                    }.disabled(deviceName.isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
        .interactiveDismissDisabled()
    }
}

#Preview {
    NameDeviceView {}
}
