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
    
    @AppStorage(Constants.preferredThemeKey)
    private var preferredTheme: ThemeType = .system
    
    @AppStorage(Constants.preferredContentAppearanceKey)
    private var preferredContentAppearance: ViewType = .list
    
    @AppStorage(Constants.preferredSortingKey)
    private var preferredSorting: SortType = .createdAt
    
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
            Picker(selection: $preferredTheme) {
                ForEach(ThemeType.allCases, id: \.self) { type in
                    Label {
                        Text(type.getUITitle())
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            } label: {
                Label {
                    Text("Settings Theme Title")
                } icon: {
                    YabaIconView(bundleKey: "paint-brush-02")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }.pickerStyle(.menu)
            HStack {
                Label {
                    Text("Settings Language Title")
                } icon: {
                    YabaIconView(bundleKey: "earth")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                Label {
                    Text(Bundle.main.preferredLocalizations[0].localizedUppercase)
                } icon: {
                    YabaIconView(bundleKey: "arrow-right-01")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                        .foregroundStyle(.tertiary)
                }.labelStyle(InverseLabelStyle())
            }
            .contentShape(Rectangle())
            .onTapGesture {
                if let url: URL = .init(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                }
            }
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
            Picker(selection: $preferredContentAppearance) {
                ForEach(ViewType.allCases, id: \.self) { type in
                    Label {
                        Text(type.getUITitle())
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            } label: {
                Label {
                    Text("Settings Content Appearance Title")
                } icon: {
                    YabaIconView(bundleKey: "change-screen-mode")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }.pickerStyle(.menu)
            Picker(selection: $preferredSorting) {
                ForEach(SortType.allCases, id: \.self) { type in
                    Label {
                        Text(type.getUITitle())
                    } icon: {
                        YabaIconView(bundleKey: type.getUIIconName())
                    }
                }
            } label: {
                Label {
                    Text("Settings Sorting Title")
                } icon: {
                    YabaIconView(bundleKey: "sorting-04")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }.pickerStyle(.menu)
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
