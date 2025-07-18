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
    
    @AppStorage(Constants.showRecentsKey)
    private var showRecents: Bool = true
    
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
            aboutSection
            thanksToSection
            #if DEBUG
            developerSection
            #endif
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
            if UIDevice.current.userInterfaceIdiom == .phone {
                Toggle(isOn: $showRecents) {
                    Label {
                        Text("Settings Recents Title")
                    } icon: {
                        YabaIconView(bundleKey: "clock-01")
                            .scaledToFit()
                            .frame(width: 24, height: 24)
                    }
                }
            }
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
    private var aboutSection: some View {
        Section {
            generateLinkableItem(
                title: LocalizedStringKey("Settings Repo Link Title"),
                iconKey: "github",
                urlToOpen: Constants.yabaRepoLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings EULA Link Title"),
                iconKey: "agreement-03",
                urlToOpen: Constants.eulaLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings ToS Link Title"),
                iconKey: "agreement-02",
                urlToOpen: Constants.tosLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings Privacy Policy Link Title"),
                iconKey: "justice-scale-02",
                urlToOpen: Constants.privacyPolicyLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings Developer Website Link Title"),
                iconKey: "developer",
                urlToOpen: Constants.developerWebsiteLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings Mailto Link Title"),
                iconKey: "mail-01",
                urlToOpen: Constants.feedbackLink
            )
            generateLinkableItem(
                title: LocalizedStringKey("Settings Store Link Title"),
                iconKey: "app-store",
                urlToOpen: Constants.storeLink
            )
        } header: {
            Label {
                Text("Settings About Title")
            } icon: {
                YabaIconView(bundleKey: "information-square")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var thanksToSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings HugeIcons Title")
                } icon: {
                    YabaIconView(bundleKey: "hugeicons")
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
            .alert(
                "Settings HugeIcons Title",
                isPresented: $settingsState.shouldShowHugeIconsAlert
            ) {
                Button {
                    settingsState.shouldShowHugeIconsAlert = false
                } label: {
                    Text("Done")
                }
            } message: {
                Text("Settings HugeIcons Description")
            }
            .dialogIcon(Image("hugeicons"))
            .onTapGesture {
                settingsState.shouldShowHugeIconsAlert = true
            }
            
            HStack {
                Label {
                    Text("Settings IconKitchen Title")
                } icon: {
                    YabaIconView(bundleKey: "knife-02")
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
            .alert(
                "Settings IconKitchen Title",
                isPresented: $settingsState.shouldShowIconKitchenAlert
            ) {
                Button {
                    settingsState.shouldShowIconKitchenAlert = false
                } label: {
                    Text("Done")
                }
            } message: {
                Text("Settings IconKitchen Description")
            }
            .dialogIcon(Image("knife-02"))
            .onTapGesture {
                settingsState.shouldShowIconKitchenAlert = true
            }
        } header: {
            Label {
                Text("Settings Thanks To Section Title")
            } icon: {
                YabaIconView(bundleKey: "champion")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var developerSection: some View {
        Section {
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
            HStack {
                Label {
                    Text("Settings Reset App Storage Label")
                } icon: {
                    YabaIconView(bundleKey: "folder-file-storage")
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
                settingsState.onResetAppStorage()
            }
        } header: {
            Label {
                Text("Settings Developer Title")
            } icon: {
                YabaIconView(bundleKey: "code")
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
            allowedContentTypes: [.json, .commaSeparatedText, .html]
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
    
    @ViewBuilder
    private func generateLinkableItem(
        title: LocalizedStringKey,
        iconKey: String,
        urlToOpen: String,
    ) -> some View {
        HStack {
            Label {
                Text(title)
            } icon: {
                YabaIconView(bundleKey: iconKey)
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
            if let url: URL = .init(string: urlToOpen), UIApplication.shared.canOpenURL(url) {
                UIApplication.shared.open(url)
            }
        }
    }
}

#Preview {
    SettingsView()
}
