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
    
    @AppStorage(Constants.showMenuBarItem)
    private var showMenuBarItem: Bool = true
    
    @AppStorage(Constants.preventDeletionSyncKey)
    private var preventDeletionSync: Bool = false
    
    @AppStorage(Constants.saveToArchiveOrgKey)
    private var saveToArchiveOrg: Bool = false
    
    @AppStorage(
        Constants.useSimplifiedShare,
        store: UserDefaults(
            suiteName: "group.dev.subfly.YABA"
        )
    )
    private var useSimplifiedShare: Bool = false
    
    @AppStorage(Constants.deviceNameKey)
    private var deviceName: String = ""
    
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
                case .previousAnnouncements: PreviousAnnouncementsView()
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
            #if !targetEnvironment(macCatalyst)
            keyboardSection
            #endif
            announcementsSection
            syncSection
            dataSection
            aboutSection
            socialsSection
            thanksToSection
            #if DEBUG
            developerSection
            #endif
        }
        .listStyle(.sidebar)
        .scrollContentBackground(.hidden)
        .background(.clear)
        .navigationTitle("Settings Title")
        .sheet(isPresented: $settingsState.shouldShowGuideSheet) {
            HowToGuideView()
        }
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
            #if !targetEnvironment(macCatalyst)
            Toggle(isOn: $useSimplifiedShare) {
                Label {
                    Text("Settings Simplified Share Sheet Title")
                } icon: {
                    YabaIconView(bundleKey: "relieved-02")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }
            #endif
            #if targetEnvironment(macCatalyst)
            Toggle(isOn: $showMenuBarItem) {
                Label {
                    Text("Settings Menu Item Visibility Title")
                } icon: {
                    YabaIconView(bundleKey: "bookmark-02")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }.onChange(of: showMenuBarItem) { _, newValue in
                if newValue {
                    StatusMenuHelper.setStatusMenuEnabled()
                } else {
                    StatusMenuHelper.setStatusMenuDisabled()
                }
            }
            #endif
            #if !targetEnvironment(macCatalyst)
            if UIDevice.current.userInterfaceIdiom == .phone {
                FABLocationPicker()
            }
            #endif
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
    private var keyboardSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings Keyboard Label")
                } icon: {
                    YabaIconView(bundleKey: "keyboard")
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
                let settingsURL = URL(string: UIApplication.openSettingsURLString)
                if let url: URL = settingsURL, UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                }
            }
        } header: {
            Label {
                Text("Settings Keyboard Title")
            } icon: {
                YabaIconView(bundleKey: "keyboard")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var announcementsSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings Announcements Title")
                } icon: {
                    YabaIconView(bundleKey: "notification-01")
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
                settingsState.onNavigateToAnnouncements()
            }
        } header: {
            Label {
                Text("Home Announcements Title")
            } icon: {
                YabaIconView(bundleKey: "megaphone-03")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var syncSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings Name Device Label")
                } icon: {
                    YabaIconView(bundleKey: DeviceType.current.symbolName)
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                TextField("", text: $deviceName)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
            Toggle(isOn: $saveToArchiveOrg) {
                Label {
                    Text("Save to Archive.org")
                } icon: {
                    YabaIconView(bundleKey: "folder-shared-01")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }
            Toggle(isOn: $preventDeletionSync) {
                Label {
                    Text("Settings Prevent Deletion Sync Label")
                } icon: {
                    YabaIconView(bundleKey: "folder-transfer")
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
            }
            .listRowBackground(
                Rectangle()
                    .fill(.thinMaterial)
            )
        } header: {
            Label {
                Text("Synchronization")
            } icon: {
                YabaIconView(bundleKey: "computer-phone-sync")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        } footer: {
            Text("Settings Prevent Deletion Sync Description")
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
    private var socialsSection: some View {
        Section {
            generateLinkableItem(
                title: LocalizedStringKey("Settings Reddit Link Title"),
                iconKey: "reddit",
                urlToOpen: Constants.officialRedditLink
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
                Text("Settings Socials Title")
            } icon: {
                YabaIconView(bundleKey: "rss")
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
        }
    }
    
    @ViewBuilder
    private var aboutSection: some View {
        Section {
            HStack {
                Label {
                    Text("Settings How To Guide Title")
                } icon: {
                    YabaIconView(bundleKey: "book-open-02")
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
                settingsState.shouldShowGuideSheet = true
            }
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
        // I just love this beautiful SwiftUI hack...
        .fileExporter(
            isPresented: $settingsState.shouldShowMarkupExportSheet,
            document: settingsState.exportableMarkupDocument,
            contentType: .plainText,
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
                Button {
                    settingsState.shouldShowExportTypeSelection = false
                    settingsState.showExportSheet(
                        using: modelContext,
                        withType: .plainText
                    )
                } label: {
                    Label {
                        Text("MARKDOWN")
                    } icon: {
                        YabaIconView(bundleKey: "file-02")
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

