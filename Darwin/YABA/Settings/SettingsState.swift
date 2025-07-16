//
//  SettingsState.swift
//  YABA
//
//  Created by Ali Taha on 23.05.2025.
//

import UniformTypeIdentifiers
import SwiftUI
import SwiftData

internal enum SettingsNavigationDestination: Hashable {
    case mapper, logs
}

@MainActor
@Observable
internal class SettingsState {
    @ObservationIgnored
    private var dataManager: DataManager = .init()
    
    let toastManager: ToastManager = .init()
    
    var settingsNavPath: [SettingsNavigationDestination] = []
    
    var showDeleteAllDialog: Bool = false
    var shouldShowImportSheet: Bool = false
    var shouldShowExportTypeSelection: Bool = false
    var shouldShowJsonExportSheet: Bool = false
    var shouldShowCsvExportSheet: Bool = false
    var shouldShowHtmlExportSheet: Bool = false
    
    var shouldShowHugeIconsAlert: Bool = false
    var shouldShowIconKitchenAlert: Bool = false
    
    var isImporting: Bool = false
    var isExporting: Bool = false
    var isDeleting: Bool = false
    
    /**
     * Holds the data in a format as:
     * URL - Label - Description - Created At
     * respectively.
     */
    var mappedHeaders: Dictionary<MappableCSVHeaderValues, Int?> = [:]
    var importedHeaders: [String] = []
    var importedFileData: Data? = nil
    var importedFileType: UTType? = nil
    
    var exportableJsonDocument: YabaExportableJsonDocument? = nil
    var exportableCsvDocument: YabaExportableCsvDocument? = nil
    var exportableHtmlDocument: YabaExportableHtmlDocument? = nil
    
    func reset() {
        mappedHeaders = [
            .url         : nil,
            .label       : nil,
            .description : nil,
            .createdAt   : nil,
        ]
        importedHeaders = []
        importedFileData = nil
        importedFileType = nil
    }
    
    func tryToImport(
        with fileUrl: URL,
        using modelContext: ModelContext
    ) {
        if fileUrl.startAccessingSecurityScopedResource() {
            guard let fileData = try? Data(contentsOf: fileUrl) else {
                toastManager.show(
                    message: LocalizedStringKey("Unable to Access File Message"),
                    accentColor: .red,
                    acceptText: LocalizedStringKey("Ok"),
                    iconType: .error,
                    onAcceptPressed: { self.toastManager.hide() }
                )
                return
            }
            
            let fileType = fileUrl.pathExtension.lowercased()
            if !(["json", "csv", "html"].contains(fileType)) {
                toastManager.show(
                    message: LocalizedStringKey("Unsupported File Type Message"),
                    accentColor: .red,
                    acceptText: LocalizedStringKey("Ok"),
                    iconType: .error,
                    onAcceptPressed: { self.toastManager.hide() }
                )
                return
            }
            
            if fileType == "json" {
                do {
                    try dataManager.importJSON(from: fileData, using: modelContext)
                    self.toastManager.show(
                        message: LocalizedStringKey("Import Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                } catch {
                    importedFileData = fileData
                    importedFileType = .json
                    settingsNavPath.append(.mapper)
                }
            } else if fileType == "csv" {
                do {
                    try dataManager.importCSV(from: fileData, using: modelContext)
                    self.toastManager.show(
                        message: LocalizedStringKey("Import Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                } catch {
                    importedFileData = fileData
                    importedFileType = .commaSeparatedText
                    importedHeaders = dataManager.extractCSVHeaders(from: fileData)
                    settingsNavPath.append(.mapper)
                }
            } else if fileType == "html" {
                do {
                    try dataManager.importHTML(from: fileData, using: modelContext)
                    self.toastManager.show(
                        message: LocalizedStringKey("Import Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                } catch {
                    importedFileData = fileData
                    importedFileType = .html
                    settingsNavPath.append(.mapper)
                }
            }
            fileUrl.stopAccessingSecurityScopedResource()
        }
    }
    
    func importFixedCSV(using modelContext: ModelContext) {
        isImporting = true
        defer { isImporting = false }
        
        do {
            try dataManager.importFixedCSV(
                using: modelContext,
                headers: mappedHeaders,
                data: importedFileData,
                onFinish: {
                    self.settingsNavPath.removeLast()
                    self.toastManager.show(
                        message: LocalizedStringKey("Import Successful Message"),
                        accentColor: .green,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .success,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                }
            )
        } catch {
            handleError(error: error)
        }
    }
    
    func showExportSheet(
        using modelContext: ModelContext,
        withType type: UTType,
    ) {
        isExporting = true
        defer { isExporting = false }
        
        do {
            try dataManager.prepareExportableData(
                using: modelContext,
                withType: type,
                onReady: { data in
                    if type == .commaSeparatedText {
                        self.exportableCsvDocument = .init(data: data)
                        self.shouldShowCsvExportSheet = true
                    } else if type == .json {
                        self.exportableJsonDocument = .init(data: data)
                        self.shouldShowJsonExportSheet = true
                    }
                }
            )
        } catch {
            handleError(error: error)
        }
    }
    
    func deleteAllData(
        using modelContext: ModelContext,
        onFinishCallback: @escaping () -> Void
    ) {
        isDeleting = true
        defer {
            showDeleteAllDialog = false
            isDeleting = false
        }
        
        dataManager.deleteAllData(
            using: modelContext,
            onFinishCallback: {
                self.toastManager.show(
                    message: LocalizedStringKey("Delete All Successful Message"),
                    accentColor: .green,
                    acceptText: LocalizedStringKey("Ok"),
                    iconType: .success,
                    onAcceptPressed: { self.toastManager.hide() }
                )
                onFinishCallback()
            }
        )
    }
    
    func onNavigateToLogs() {
        settingsNavPath.append(.logs)
    }
    
    func onResetAppStorage() {
        @AppStorage(Constants.hasPassedOnboardingKey)
        var hasPassedOnboarding = false
        
        @AppStorage(Constants.preferredThemeKey)
        var theme: ThemeType = .system
        
        @AppStorage(Constants.preferredContentAppearanceKey)
        var contentAppearance: ViewType = .list
        
        @AppStorage(Constants.preferredCardImageSizingKey)
        var imageSizing: CardViewTypeImageSizing = .small
        
        @AppStorage(Constants.preferredSortingKey)
        var sortType: SortType = .createdAt
        
        @AppStorage(Constants.preferredSortOrderKey)
        var sortOrderType: SortOrderType = .ascending
        
        hasPassedOnboarding = false
        theme = .system
        contentAppearance = .list
        imageSizing = .small
        sortType = .createdAt
        sortOrderType = .ascending
    }
    
    private func handleError(error: Error) {
        if let dataError = error as? DataError {
            switch dataError {
            case .invalidCSVEncoding(let message),
                    .emptyCSV(let message),
                    .emptyJSON(let message),
                    .emptyHTML(let message),
                    .exportableGenerationError(let message),
                    .caseURLColumnNotSelected(let message),
                    .invalidBookmarkUrl(let message),
                    .invalidHTML(let message),
                    .unkownError(let message):
                toastManager.show(
                    message: message,
                    accentColor: .red,
                    acceptText: LocalizedStringKey("Ok"),
                    iconType: .error,
                    onAcceptPressed: { self.toastManager.hide() }
                )
            }
        } else {
            toastManager.show(
                message: LocalizedStringKey("Data Manager Unknown Error Message"),
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
        }
    }
}
