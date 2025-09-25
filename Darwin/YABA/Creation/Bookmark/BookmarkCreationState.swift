//
//  BookmarkCreationState.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//
//  Debounce logic taken from: https://stackoverflow.com/questions/76887491/swift-observation-framework-and-debounce

import Foundation
import SwiftUI
import Combine
import SwiftData
import WidgetKit

@MainActor
@Observable
internal class BookmarkCreationState {
    @ObservationIgnored
    private let unfurler: Unfurler = .init()
    @ObservationIgnored
    private var debouncedUrl = CurrentValueSubject<String, Never>("")
    @ObservationIgnored
    private var cancels = Set<AnyCancellable>()
    @ObservationIgnored
    var lastFetchedUrl: String = ""
    
    var url: String = "" {
        didSet {
            debouncedUrl.send(url)
        }
    }
    var cleanerUrl: String = ""
    var host: String = ""
    var label: String = ""
    var description: String = ""
    var iconURL: String? = nil
    var imageURL: String? = nil
    var videoURL: String? = nil
    var imageData: Data? = nil
    var iconData: Data? = nil
    var readableHTML: String? = nil
    
    var selectedType: BookmarkType = .none
    var selectedFolder: YabaCollection? = nil
    var selectedTags: [YabaCollection] = []
    
    var isLoading: Bool = false
    var hasError: Bool = false
    
    var contentAppearance: PreviewContentAppearance = .list
    
    var uncatagroizedFolderCreationRequired: Bool = false
    
    let toastManager: ToastManager = .init()
    
    @ObservationIgnored
    let isInEditMode: Bool
    
    init(isInEditMode: Bool) {
        self.isInEditMode = isInEditMode
    }
    
    func fetchData(with urlString: String) async {
        if urlString.isEmpty || urlString == lastFetchedUrl {
            return
        }
        
        isLoading = true
        defer {
            isLoading = false
        }
        
        do {
            if let fetched = try await unfurler.unfurl(urlString: urlString) {
                self.host = fetched.host ?? ""
                if label.isEmpty {
                    self.label = fetched.title ?? ""
                }
                if description.isEmpty {
                    self.description = fetched.description ?? ""
                }
                self.iconURL = fetched.iconURL
                self.imageURL = fetched.imageURL
                self.videoURL = fetched.videoURL
                self.readableHTML = fetched.readableHTML
                self.iconData = fetched.iconData
                self.imageData = fetched.imageData
                self.cleanerUrl = fetched.url
            }
            hasError = false
            lastFetchedUrl = urlString
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Success Text"),
                accentColor: .green,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .success,
                onAcceptPressed: { self.toastManager.hide() }
            )
        } catch UnfurlError.cannotCreateURL(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        } catch UnfurlError.unableToUnfurl(let message) {
            toastManager.show(
                message: message,
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        } catch {
            toastManager.show(
                message: LocalizedStringKey("Generic Unfurl Error Text"),
                accentColor: .red,
                acceptText: LocalizedStringKey("Ok"),
                iconType: .error,
                onAcceptPressed: { self.toastManager.hide() }
            )
            hasError = true
        }
    }
    
    func listenUrlChanges(_ perform: @escaping (String) -> Void) {
        debouncedUrl.debounce(for: 0.75, scheduler: RunLoop.main)
            .dropFirst(isInEditMode ? 1 : 0)
            .sink { perform($0) }
            .store(in: &cancels)
    }
    
    func onDone(
        bookmarkToEdit: YabaBookmark?,
        using modelContext: ModelContext,
        onFinishCallback: @escaping () -> Void,
        saveToArchiveOrg: Bool
    ) {
        guard let selectedFolder else { return }
        
        withAnimation {
            if let bookmarkToEdit {
                bookmarkToEdit.label = label
                bookmarkToEdit.link = cleanerUrl
                bookmarkToEdit.domain = host
                bookmarkToEdit.bookmarkDescription = description
                bookmarkToEdit.imageUrl = imageURL
                bookmarkToEdit.iconUrl = iconURL
                bookmarkToEdit.videoUrl = videoURL
                bookmarkToEdit.readableHTML = readableHTML
                bookmarkToEdit.type = selectedType.rawValue
                bookmarkToEdit.iconDataHolder = iconData
                bookmarkToEdit.imageDataHolder = imageData
                bookmarkToEdit.editedAt = .now
                bookmarkToEdit.version += 1
                
                // Folder changing
                if let collections = bookmarkToEdit.collections {
                    if let lastSelectedFolderIndex = collections.firstIndex(
                        where: { $0.collectionType == .folder }
                    ) {
                        let lastSelectedFolder = collections[lastSelectedFolderIndex]
                        if lastSelectedFolder.hasChanges(with: selectedFolder) {
                            bookmarkToEdit.collections?.remove(at: lastSelectedFolderIndex)
                            bookmarkToEdit.collections?.append(selectedFolder)
                        }
                    }
                }
                
                // Tags changing
                bookmarkToEdit.collections?.removeAll { $0.collectionType == .tag }
                bookmarkToEdit.collections?.append(contentsOf: selectedTags)
            } else {
                var collections = selectedTags
                collections.append(selectedFolder)

                let creationTime: Date = .now
                let newBookmark = YabaBookmark(
                    bookmarkId: UUID().uuidString,
                    link: cleanerUrl,
                    label: label,
                    bookmarkDescription: description,
                    domain: host,
                    createdAt: creationTime,
                    editedAt: creationTime,
                    imageDataHolder: imageData,
                    iconDataHolder: iconData,
                    imageUrl: imageURL,
                    iconUrl: iconURL,
                    videoUrl: videoURL,
                    readableHTML: readableHTML,
                    type: selectedType,
                    version: 0,
                    collections: collections
                )

                modelContext.insert(newBookmark)
            }

            let bookmarkToArchive: YabaBookmark
            if let bookmarkToEdit {
                bookmarkToArchive = bookmarkToEdit
            } else {
                // For new bookmarks, we need to find the one we just created
                // Since we can't easily reference it, let's fetch it by recent creation
                let descriptor = FetchDescriptor<YabaBookmark>(
                    predicate: #Predicate<YabaBookmark> { bookmark in
                        bookmark.link == cleanerUrl
                    },
                    sortBy: [SortDescriptor(\YabaBookmark.createdAt, order: .reverse)]
                )
                guard let newBookmark = try? modelContext.fetch(descriptor).first else {
                    // If we can't find it, skip archiving
                    return
                }
                bookmarkToArchive = newBookmark
            }
            try? modelContext.save()

            // Close the bookmark creation panel immediately
            onFinishCallback()

            // Archive to archive.org in background if toggle is enabled
            if saveToArchiveOrg {
                // Archive all bookmarks when enabled, in background
                archiveURLInBackground(cleanerUrl, bookmark: bookmarkToArchive, modelContext: modelContext)
            }
        }
        
        WidgetCenter.shared.reloadAllTimelines()
    }
    
    func onAppear(
        link: String?,
        bookmarkToEdit: YabaBookmark?,
        collectionToFill: YabaCollection?,
        storedContentAppearance: ViewType,
        storedCardImageSizing: CardViewTypeImageSizing,
        using modelContext: ModelContext
    ) async {
        /// MARK: PREVIEW TYPE SELECTION
        switch storedContentAppearance {
        case .list:
            contentAppearance = .list
        case .card:
            switch storedCardImageSizing {
            case .big:
                contentAppearance = .cardBigImage
            case .small:
                contentAppearance = .cardSmallImage
            }
        }
        
        /// MARK: BOOKMARK FOLDER FILLING
        var folderToFill = bookmarkToEdit?.collections?.first(where: { $0.collectionType == .folder })
        if folderToFill == nil {
            let id = Constants.uncategorizedCollectionId // Thanks #Predicate for that...
            let descriptor = FetchDescriptor<YabaCollection>(
                predicate: #Predicate<YabaCollection> { collection in
                    collection.collectionId == id
                }
            )
            if let existingUncatagorizedFolder = try? modelContext.fetch(descriptor).first {
                folderToFill = existingUncatagorizedFolder
            } else {
                let creationTime = Date.now
                folderToFill = YabaCollection(
                    collectionId: Constants.uncategorizedCollectionId,
                    label: Constants.uncategorizedCollectionLabelKey,
                    icon: "folder-01",
                    createdAt: creationTime,
                    editedAt: creationTime,
                    color: .none,
                    type: .folder,
                    version: 0,
                )
                uncatagroizedFolderCreationRequired = true
            }
        }
        
        /// MARK: BOOKMARK DATA FILLER FOR VM
        if let bookmarkToEdit {
            url = bookmarkToEdit.link
            cleanerUrl = bookmarkToEdit.link
            label = bookmarkToEdit.label
            description = bookmarkToEdit.bookmarkDescription
            host = bookmarkToEdit.domain
            imageData = bookmarkToEdit.imageDataHolder
            iconData = bookmarkToEdit.iconDataHolder
            videoURL = bookmarkToEdit.videoUrl
            readableHTML = bookmarkToEdit.readableHTML
            selectedType = bookmarkToEdit.bookmarkType
            selectedFolder = bookmarkToEdit.collections?.first(where: { $0.collectionType == .folder })
            selectedTags = bookmarkToEdit.collections?.filter { $0.collectionType == .tag } ?? []
        } else {
            selectedFolder = folderToFill
        }
        
        /// MARK: BOOKMARK URL FILLER FOR SHARE EXTENSIONS
        if let link {
            url = link
        }
        
        /// MARK: COLLECTION FILLING
        if let collectionToFill {
            switch collectionToFill.collectionType {
            case .folder:
                selectedFolder = collectionToFill
            case .tag:
                selectedTags.append(collectionToFill)
            }
        }
    }
    
    func simpleOnAppear(
        link: String?,
        bookmarkToEdit: YabaBookmark?,
        collectionToFill: YabaCollection?,
        using modelContext: ModelContext
    ) async {
        /// MARK: BOOKMARK FOLDER FILLING
        let id = Constants.uncategorizedCollectionId // Thanks #Predicate for that...
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate<YabaCollection> { collection in
                collection.collectionId == id
            }
        )
        
        if let existingUncatagorizedFolder = try? modelContext.fetch(descriptor).first {
            selectedFolder = existingUncatagorizedFolder
        } else {
            let creationTime = Date.now
            selectedFolder = YabaCollection(
                collectionId: Constants.uncategorizedCollectionId,
                label: Constants.uncategorizedCollectionLabelKey,
                icon: "folder-01",
                createdAt: creationTime,
                editedAt: creationTime,
                color: .none,
                type: .folder,
                version: 0
            )
            uncatagroizedFolderCreationRequired = true
        }
        
        /// MARK: BOOKMARK URL FILLER FOR SHARE EXTENSIONS
        if let link {
            url = link
        }
        
        /// MARK: COLLECTION FILLING
        if let collectionToFill {
            switch collectionToFill.collectionType {
            case .folder:
                selectedFolder = collectionToFill
            case .tag:
                selectedTags.append(collectionToFill)
            }
        }
    }
    
    func onClearTitle() {
        label = ""
    }

    // Helper function to find or create a tag
    private func findOrCreateTag(label: String, using modelContext: ModelContext) -> YabaCollection {
        let descriptor = FetchDescriptor<YabaCollection>(
            predicate: #Predicate<YabaCollection> { collection in
                collection.label == label && collection.type == 2
            }
        )
        if let existingTag = try? modelContext.fetch(descriptor).first {
            return existingTag
        } else {
            let newTag = YabaCollection(
                collectionId: UUID().uuidString,
                label: label,
                icon: "tag-01",
                createdAt: .now,
                editedAt: .now,
                color: .none,
                type: .tag,
                version: 0
            )
            modelContext.insert(newTag)
            return newTag
        }
    }

    // Helper function to add tag to bookmark, avoiding duplicates and removing opposite
    private func addTagToBookmark(_ tagLabel: String, removeOpposite: String?, bookmark: YabaBookmark, using modelContext: ModelContext) {
        let tag = findOrCreateTag(label: tagLabel, using: modelContext)

        // Remove opposite tag if it exists
        if let oppositeLabel = removeOpposite {
            bookmark.collections?.removeAll { collection in
                collection.label == oppositeLabel && collection.collectionType == .tag
            }
        }

        // Add tag if not already present
        if !(bookmark.collections?.contains(where: { $0.label == tagLabel && $0.collectionType == .tag }) ?? false) {
            bookmark.collections?.append(tag)
        }

        try? modelContext.save()
    }
    
    private func archiveURLInBackground(_ url: String, bookmark: YabaBookmark, modelContext: ModelContext) {
        // Start background archiving with retry mechanism
        Task.detached { [weak self] in
            await self?.performArchiveWithRetry(url: url, bookmark: bookmark, modelContext: modelContext, attempt: 1, maxAttempts: 3)
        }
    }
    
    private func performArchiveWithRetry(url: String, bookmark: YabaBookmark, modelContext: ModelContext, attempt: Int, maxAttempts: Int) async {
        print("YABA Archive Debug: Attempt \(attempt)/\(maxAttempts) - Starting to archive URL: \(url)")
        
        do {
            // Use the correct archive.org endpoint format: https://web.archive.org/save/[URL]
            // Based on JavaScript: javascript:void(window.open('https://web.archive.org/save/'+location.href));
            let archiveURLString = "https://web.archive.org/save/\(url)"
            
            guard let archiveURL = URL(string: archiveURLString) else {
                print("YABA Archive Debug: Failed to create archive URL: \(archiveURLString)")
                await MainActor.run {
                    // Add "unarchived" tag and remove "archived" tag if present
                    addTagToBookmark("unarchived", removeOpposite: "archived", bookmark: bookmark, using: modelContext)

                    toastManager.show(
                        message: LocalizedStringKey("Failed to archive link - invalid URL"),
                        accentColor: .red,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .error,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                }
                return
            }
            
            print("YABA Archive Debug: Archive URL: \(archiveURLString)")
            
            // Use GET request (not POST) as per archive.org API
            var request = URLRequest(url: archiveURL)
            request.httpMethod = "GET"
            request.timeoutInterval = 60.0 // Increase timeout for archive.org processing
            
            // Add user agent to avoid blocking
            request.setValue("YABA-BookmarkApp/1.0", forHTTPHeaderField: "User-Agent")
            
            print("YABA Archive Debug: Sending GET request to archive.org")
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            print("YABA Archive Debug: Received response from archive.org")
            
            if let httpResponse = response as? HTTPURLResponse {
                print("YABA Archive Debug: HTTP Status Code: \(httpResponse.statusCode)")
                
                if httpResponse.statusCode == 200 || httpResponse.statusCode == 302 || httpResponse.statusCode == 301 {
                    // Success (200), redirect (302/301) - all indicate successful archiving
                    print("YABA Archive Debug: Successfully archived URL on attempt \(attempt)")
                    await MainActor.run {
                        // Add "archived" tag and remove "unarchived" tag if present
                        addTagToBookmark("archived", removeOpposite: "unarchived", bookmark: bookmark, using: modelContext)

                        toastManager.show(
                            message: LocalizedStringKey("Link archived successfully"),
                            accentColor: .green,
                            acceptText: LocalizedStringKey("Ok"),
                            iconType: .success,
                            onAcceptPressed: { self.toastManager.hide() }
                        )
                    }
                    return // Success, no need to retry
                } else {
                    // Error response - try to retry
                    print("YABA Archive Debug: Archive.org returned error status: \(httpResponse.statusCode) on attempt \(attempt)")
                    if attempt < maxAttempts {
                        // Wait before retrying (exponential backoff)
                        let delay = UInt64(pow(2.0, Double(attempt)) * 1_000_000_000) // 2^attempt seconds
                        try await Task.sleep(nanoseconds: delay)
                        await performArchiveWithRetry(url: url, bookmark: bookmark, modelContext: modelContext, attempt: attempt + 1, maxAttempts: maxAttempts)
                        return
                    } else {
                        // Max attempts reached
                        await MainActor.run {
                            // Add "unarchived" tag and remove "archived" tag if present
                            addTagToBookmark("unarchived", removeOpposite: "archived", bookmark: bookmark, using: modelContext)

                            toastManager.show(
                                message: LocalizedStringKey("Failed to archive link after \(maxAttempts) attempts - server error (\(httpResponse.statusCode))"),
                                accentColor: .red,
                                acceptText: LocalizedStringKey("Ok"),
                                iconType: .error,
                                onAcceptPressed: { self.toastManager.hide() }
                            )
                        }
                    }
                }
            } else {
                print("YABA Archive Debug: No HTTP response received on attempt \(attempt)")
                if attempt < maxAttempts {
                    // Wait before retrying
                    let delay = UInt64(pow(2.0, Double(attempt)) * 1_000_000_000) // 2^attempt seconds
                    try await Task.sleep(nanoseconds: delay)
                    await performArchiveWithRetry(url: url, bookmark: bookmark, modelContext: modelContext, attempt: attempt + 1, maxAttempts: maxAttempts)
                    return
                } else {
                    await MainActor.run {
                        // Add "unarchived" tag and remove "archived" tag if present
                        addTagToBookmark("unarchived", removeOpposite: "archived", bookmark: bookmark, using: modelContext)

                        toastManager.show(
                            message: LocalizedStringKey("Failed to archive link after \(maxAttempts) attempts - no response"),
                            accentColor: .red,
                            acceptText: LocalizedStringKey("Ok"),
                            iconType: .error,
                            onAcceptPressed: { self.toastManager.hide() }
                        )
                    }
                }
            }
        } catch {
            print("YABA Archive Debug: Network error on attempt \(attempt): \(error.localizedDescription)")
            if attempt < maxAttempts {
                // Wait before retrying
                let delay = UInt64(pow(2.0, Double(attempt)) * 1_000_000_000) // 2^attempt seconds
                try? await Task.sleep(nanoseconds: delay)
                await performArchiveWithRetry(url: url, bookmark: bookmark, modelContext: modelContext, attempt: attempt + 1, maxAttempts: maxAttempts)
            } else {
                await MainActor.run {
                    // Add "unarchived" tag and remove "archived" tag if present
                    addTagToBookmark("unarchived", removeOpposite: "archived", bookmark: bookmark, using: modelContext)

                    toastManager.show(
                        message: LocalizedStringKey("Failed to archive link after \(maxAttempts) attempts - network error"),
                        accentColor: .red,
                        acceptText: LocalizedStringKey("Ok"),
                        iconType: .error,
                        onAcceptPressed: { self.toastManager.hide() }
                    )
                }
            }
        }
    }
}
