//
//  SettingsView.swift
//  YABA
//
//  Created by Ali Taha on 2.05.2025.
//

import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.dismiss)
    private var dismiss
    
    @Environment(\.modelContext)
    private var modelContext
    
    @Environment(\.appState)
    private var appState
    
    @AppStorage(Constants.preferredThemeKey)
    private var preferredTheme: ThemeType = .system
    
    @State
    private var settingsState = SettingsState()
    
    var body: some View {
        NavigationStack(path: $settingsState.settingsNavPath) {
            ZStack {
                AnimatedGradient(collectionColor: .accentColor)
                #if targetEnvironment(macCatalyst)
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
                #else
                content
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button {
                                dismiss()
                            } label: {
                                if UIDevice.current.userInterfaceIdiom == .pad {
                                    Text("Done")
                                } else {
                                    YabaIconView(bundleKey: "cancel-01")
                                }
                            }
                        }
                    }
                #endif
            }
            .navigationDestination(for: SettingsNavigationDestination.self) { destination in
                switch destination {
                    case .mapper: MapperView(settingsState: $settingsState)
                        .navigationBarBackButtonHidden()
                    case .logs: EventsLogView()
                        .navigationBarBackButtonHidden()
                }
            }
        }
        .preferredColorScheme(preferredTheme.getScheme())
        .toast(
            state: settingsState.toastManager.toastState,
            isShowing: settingsState.toastManager.isShowing,
            onDismiss: {
                settingsState.toastManager.hide()
            }
        )
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
            importButton
            exportButton
            deleteAllButton
            #if DEBUG
            HStack {
                Label {
                    Text("Settings Event Logs Label")
                } icon: {
                    YabaIconView(bundleKey: "calendar-03")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Spacer()
                YabaIconView(bundleKey: "arrow-right-01")
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                    .foregroundStyle(.tertiary)
            }
            .contentShape(Rectangle())
            .onTapGesture {
                settingsState.onNavigateToLogs()
            }
            #endif
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
    
    @ViewBuilder
    private var importButton: some View {
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
        .contentShape(Rectangle())
        .onTapGesture {
            settingsState.shouldShowImportSheet = true
        }
        .fileImporter(
            isPresented: $settingsState.shouldShowImportSheet,
            allowedContentTypes: [.json, .commaSeparatedText]
        ) { result in
            switch result {
            case .success(let file):
                settingsState.tryToImport(
                    with: file,
                    using: modelContext
                )
            case .failure:
                settingsState.toastManager.show(
                    message: LocalizedStringKey("Unable to Access File Message"),
                    accentColor: .red,
                    acceptText: LocalizedStringKey("Ok"),
                    iconType: .error,
                    onAcceptPressed: { settingsState.toastManager.hide() }
                )
            }
        }
    }
    
    @ViewBuilder
    private var exportButton: some View {
        HStack {
            Label {
                HStack {
                    Text("Settings Export Title")
                    if settingsState.isExporting {
                        ProgressView().controlSize(.regular)
                    }
                }
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
        .contentShape(Rectangle())
        .onTapGesture {
            settingsState.shouldShowExportTypeSelection = true
        }
        .confirmationDialog(
            "Export Type Selection Label",
            isPresented: $settingsState.shouldShowExportTypeSelection,
            actions: {
                Button {
                    settingsState.shouldShowExportTypeSelection = false
                    settingsState.showExportSheet(
                        using: modelContext,
                        withType: .json
                    )
                } label: {
                    Label {
                        Text("JSON")
                    } icon: {
                        YabaIconView(bundleKey: "file-script")
                    }
                }
                Button {
                    settingsState.shouldShowExportTypeSelection = false
                    settingsState.showExportSheet(
                        using: modelContext,
                        withType: .commaSeparatedText
                    )
                } label: {
                    Label {
                        Text("CSV")
                    } icon: {
                        YabaIconView(bundleKey: "csv-02")
                    }
                }
            }
        )
        .fileExporter(
            isPresented: $settingsState.shouldShowCsvExportSheet,
            document: settingsState.exportableCsvDocument,
            contentType: .commaSeparatedText,
            defaultFilename: "yaba_export",
            onCompletion: { result in
                switch result {
                case .success:
                    settingsState.toastManager.show(
                        message: LocalizedStringKey("Export Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { settingsState.toastManager.hide() }
                    )
                case .failure:
                    settingsState.toastManager.show(
                        message: LocalizedStringKey("Export Error Message"),
                        accentColor: .red,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .error,
                        onAcceptPressed: { settingsState.toastManager.hide() }
                    )
                }
            }
        )
    }
    
    @ViewBuilder
    private var deleteAllButton: some View {
        HStack {
            Label {
                HStack {
                    Text("Settings Delete All Title")
                        .foregroundStyle(.red)
                    if settingsState.isDeleting {
                        ProgressView().controlSize(.regular)
                    }
                }
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
        .contentShape(Rectangle())
        .onTapGesture {
            settingsState.showDeleteAllDialog = true
        }
        .alert(
            "Delete All Dialog Label",
            isPresented: $settingsState.showDeleteAllDialog,
            actions: {
                Button(role: .cancel) {
                    settingsState.showDeleteAllDialog = false
                } label: {
                    Text("Cancel")
                }
                Button(role: .destructive) {
                    settingsState.deleteAllData(
                        using: modelContext,
                        onFinishCallback: {
                            withAnimation {
                                appState.selectedBookmark = nil
                                appState.selectedCollection = nil
                            }
                        }
                    )
                } label: {
                    Text("Delete")
                }
            },
            message: {
                Text("Delete All Dialog Message")
            }
        )
        // Best hack I have ever did in this project. Thanks Apple for non-stackable File Exporters...
        .fileExporter(
            isPresented: $settingsState.shouldShowJsonExportSheet,
            document: settingsState.exportableJsonDocument,
            contentType: .json,
            defaultFilename: "yaba_export",
            onCompletion: { result in
                switch result {
                case .success:
                    settingsState.toastManager.show(
                        message: LocalizedStringKey("Export Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { settingsState.toastManager.hide() }
                    )
                case .failure:
                    settingsState.toastManager.show(
                        message: LocalizedStringKey("Export Error Message"),
                        accentColor: .red,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .error,
                        onAcceptPressed: { settingsState.toastManager.hide() }
                    )
                }
            }
        )
    }
}

#Preview {
    SettingsView()
}
