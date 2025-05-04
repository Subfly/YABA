//
//  SettingsView.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    var body: some View {
        ZStack {
            AnimatedMeshGradient(collectionColor: .accentColor)
            #if targetEnvironment(macCatalyst)
            NavigationView {
                content
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button {
                                dismiss()
                            } label: {
                                Text("Done")
                            }
                        }
                    }
            }
            #else
            content
            #endif
        }
    }
    
    @ViewBuilder
    private var content: some View {
        List {
            themeAndLangaugeSection
            appearanceSection
            dataSection
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
        .background(.clear)
        .navigationTitle("Settings Title")
    }
    
    @ViewBuilder
    private var themeAndLangaugeSection: some View {
        Section {
            ThemePicker()
            LanguagePicker()
        } header: {
            Label {
                Text("Settings Theme And Language Title")
            } icon: {
                YabaIconView(bundleKey: "account-setting-01")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var appearanceSection: some View {
        Section {
            ContentAppearancePicker()
            SortingPicker()
        } header: {
            Label {
                Text("Settings Appearance Title")
            } icon: {
                YabaIconView(bundleKey: "dashboard-square-02")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var dataSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings Import Title")
                } icon: {
                    YabaIconView(bundleKey: "file-import")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
            HStack {
                Label {
                    Text("Settings Export Title")
                } icon: {
                    YabaIconView(bundleKey: "file-export")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
            HStack {
                Label {
                    Text("Settings Delete All Title")
                        .foregroundStyle(.red)
                } icon: {
                    YabaIconView(bundleKey: "delete-02")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                        .tint(.red)
                }
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
        } header: {
            Label {
                Text("Settings Data Title")
            } icon: {
                YabaIconView(bundleKey: "database")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
}

#Preview {
    SettingsView()
}
